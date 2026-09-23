package com.example.fsa_gov.service;

import com.example.fsa_gov.client.FsaClient;
import com.example.fsa_gov.dto.async.AsyncErrorEntry;
import com.example.fsa_gov.dto.async.AsyncFindDocRequest;
import com.example.fsa_gov.dto.async.AsyncFindDocResponse;
import com.example.fsa_gov.dto.async.AsyncStatusResponse;
import com.example.fsa_gov.dto.async.FindDocItem;
import com.example.fsa_gov.parser.CertificateListParser;
import com.example.fsa_gov.parser.dto.ParseResult;
import com.example.fsa_gov.util.JsonlGzReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateImportService {

    private final CertificateListParser parser;
    private final FsaClient fsaClient;
    private final JsonlGzReader jsonlGzReader;

    /** Таймаут ожидания завершения async-запроса, мс */
    private static final long POLL_TIMEOUT_MS = 60_000;
    /** Интервал опроса статуса, мс */
    private static final long POLL_INTERVAL_MS = 2_000;

    // ===== Публичные методы =====

    public ParseResult parseLines(List<String> lines) {
        ParseResult result = parser.parse(lines);

        log.info("Парсинг завершён: РФ={}, ЕАЭС={}, некорректных={}",
                result.getRfCertificates().size(),
                result.getEaeuCertificates().size(),
                result.getInvalidEntries().size());

        result.getInvalidEntries().forEach(line ->
                log.warn("Некорректная строка: '{}'", line));

        return result;
    }

    public ImportResult startImport(ParseResult result) {
        ImportResult importResult = new ImportResult();

        if (!result.getRfCertificates().isEmpty()) {
            try {
                AsyncFindDocResponse resp = fsaClient.asyncRssFindDoc(
                        buildRequest(result.getRfCertificates()));
                importResult.setRfRequestId(resp.getRequestId());
                log.info("Запущен поиск СС РФ: {} документов, requestId={}",
                        result.getRfCertificates().size(), resp.getRequestId());
            } catch (Exception e) {
                log.error("Ошибка запуска asyncRssFindDoc", e);
                importResult.setRfError(e.getMessage());
            }
        }

        if (!result.getEaeuCertificates().isEmpty()) {
            try {
                AsyncFindDocResponse resp = fsaClient.asyncReaeuFindDoc(
                        buildRequest(result.getEaeuCertificates()));
                importResult.setEaeuRequestId(resp.getRequestId());
                log.info("Запущен поиск ЕАЭС: {} документов, requestId={}",
                        result.getEaeuCertificates().size(), resp.getRequestId());
            } catch (Exception e) {
                log.error("Ошибка запуска asyncReaeuFindDoc", e);
                importResult.setEaeuError(e.getMessage());
            }
        }

        return importResult;
    }

    /**
     * Запускает импорт, ждёт результат, делает fallback для not_found.
     */
    public ImportResult startImportWithFallback(ParseResult parsed) {
        ImportResult result = new ImportResult();

        // 1. Запускаем оба запроса
        if (!parsed.getRfCertificates().isEmpty()) {
            result.setRfRequestId(fsaClient.asyncRssFindDoc(
                    buildRequest(parsed.getRfCertificates())
            ).getRequestId());
        }
        if (!parsed.getEaeuCertificates().isEmpty()) {
            result.setEaeuRequestId(fsaClient.asyncReaeuFindDoc(
                    buildRequest(parsed.getEaeuCertificates())
            ).getRequestId());
        }

        // 2. Ждём завершения
        waitForCompletion(result.getRfRequestId());
        waitForCompletion(result.getEaeuRequestId());

        // 3. Скачиваем ошибки
        List<AsyncErrorEntry> rfErrors   = readErrors(result.getRfRequestId());
        List<AsyncErrorEntry> eaeuErrors = readErrors(result.getEaeuRequestId());

        // 4. Fallback: РФ not_found → в ЕАЭС, ЕАЭС not_found → в РФ
        List<String> rfNotFound   = extractNotFound(rfErrors);
        List<String> eaeuNotFound = extractNotFound(eaeuErrors);

        if (!rfNotFound.isEmpty()) {
            log.info("Fallback: {} РФ-документов не найдены, проверяем в ЕАЭС", rfNotFound.size());
            result.setFallbackEaeuRequestId(
                    fsaClient.asyncReaeuFindDoc(buildRequest(rfNotFound)).getRequestId());
        }
        if (!eaeuNotFound.isEmpty()) {
            log.info("Fallback: {} ЕАЭС-документов не найдены, проверяем в РФ", eaeuNotFound.size());
            result.setFallbackRfRequestId(
                    fsaClient.asyncRssFindDoc(buildRequest(eaeuNotFound)).getRequestId());
        }

        return result;
    }

    // ===== Приватные helpers =====

    /** Собрать AsyncFindDocRequest из списка номеров */
    private AsyncFindDocRequest buildRequest(List<String> numbers) {
        return AsyncFindDocRequest.builder()
                .items(numbers.stream()
                        .map(num -> new FindDocItem(num, null, null))
                        .toList())
                .build();
    }

    /**
     * Ждёт завершения обработки запроса (polling до SUCCESS/FAILED).
     * Молча выходит, если requestId == null или таймаут истёк.
     */
    private void waitForCompletion(String requestId) {
        if (requestId == null) return;

        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                AsyncStatusResponse status = fsaClient.asyncRequestStatus(requestId);

                // ← ВОТ ЗДЕСЬ добавляем нормализацию
                String s = status.getStatus() == null ? "" : status.getStatus().trim();

                log.debug("Статус {}: {}", requestId, s);

                if ("SUCCESS".equals(s) || "FAILED".equals(s)) {
                    return;
                }
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Ожидание статуса {} прервано", requestId);
                return;
            } catch (Exception e) {
                log.error("Ошибка при опросе статуса {}", requestId, e);
                return;
            }
        }
        log.warn("Таймаут ожидания статуса {} ({} мс)", requestId, POLL_TIMEOUT_MS);
    }

    /**
     * Скачивает и распаковывает файл ошибок.
     * Возвращает пустой список, если ошибок нет или что-то пошло не так.
     */
    private List<AsyncErrorEntry> readErrors(String requestId) {
        if (requestId == null) return List.of();

        try {
            AsyncStatusResponse status = fsaClient.asyncRequestStatus(requestId);
            if (status.getErrorsAvailable() == null || !status.getErrorsAvailable()) {
                return List.of();
            }
            byte[] gz = fsaClient.asyncRequestErrors(requestId);
            return jsonlGzReader.read(gz, AsyncErrorEntry.class);
        } catch (Exception e) {
            log.error("Ошибка чтения ошибок для {}", requestId, e);
            return List.of();
        }
    }

    /**
     * Скачивает и распаковывает файл результата.
     * Возвращает пустой список, если результата нет.
     */
    public <T> List<T> readResult(String requestId, Class<T> clazz) {
        if (requestId == null) return List.of();

        try {
            AsyncStatusResponse status = fsaClient.asyncRequestStatus(requestId);
            if (status.getResultAvailable() == null || !status.getResultAvailable()) {
                return List.of();
            }
            byte[] gz = fsaClient.asyncRequestResult(requestId);
            return jsonlGzReader.read(gz, clazz);
        } catch (Exception e) {
            log.error("Ошибка чтения результата для {}", requestId, e);
            return List.of();
        }
    }

    /** Оставляет только записи с reason = "not_found" */
    private List<String> extractNotFound(List<AsyncErrorEntry> errors) {
        List<String> result = new ArrayList<>();
        for (AsyncErrorEntry e : errors) {
            if ("not_found".equals(e.getReason()) && e.getNumberDoc() != null) {
                result.add(e.getNumberDoc());
            }
        }
        return result;
    }
}