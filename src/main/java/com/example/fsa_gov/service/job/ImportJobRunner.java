package com.example.fsa_gov.service.job;

import com.example.fsa_gov.client.FsaClient;
import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.dto.async.*;
import com.example.fsa_gov.parser.dto.ParseResult;
import com.example.fsa_gov.util.JsonlGzReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Асинхронный воркер: запускает запросы, ждёт статусы, делает fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportJobRunner {

    private final FsaClient fsaClient;
    private final JsonlGzReader jsonlGzReader;
    private final ImportJobStore jobStore;

    private static final long POLL_TIMEOUT_MS = 120_000; // 2 мин
    private static final long POLL_INTERVAL_MS = 2_000;

    @Async("importExecutor")
    public void run(ImportJobState state, ParseResult parsed) {
        try {
            state.setStatus(ImportJobState.JobStatus.RUNNING);
            state.setInvalidEntries(parsed.getInvalidEntries());
            jobStore.update(state);

            // 1. Запускаем оба запроса
            if (!parsed.getRfCertificates().isEmpty()) {
                state.setRfRequestId(fsaClient.asyncRssFindDoc(
                        buildRequest(parsed.getRfCertificates())).getRequestId());
            }
            if (!parsed.getEaeuCertificates().isEmpty()) {
                state.setEaeuRequestId(fsaClient.asyncReaeuFindDoc(
                        buildRequest(parsed.getEaeuCertificates())).getRequestId());
            }
            jobStore.update(state);

            // 2. Ждём завершения
            waitForCompletion(state.getRfRequestId());
            waitForCompletion(state.getEaeuRequestId());

            // 3. Читаем результаты
            List<CertificateResponseDto> rfFound = readResult(state.getRfRequestId());
            List<CertificateResponseDto> eaeuFound = readResult(state.getEaeuRequestId());

            // 4. Читаем ошибки
            List<AsyncErrorEntry> rfErrors = readErrors(state.getRfRequestId());
            List<AsyncErrorEntry> eaeuErrors = readErrors(state.getEaeuRequestId());

            // 5. Fallback
            List<String> rfNotFound = extractNotFound(rfErrors);
            List<String> eaeuNotFound = extractNotFound(eaeuErrors);
            state.setRfNotFound(rfNotFound);
            state.setEaeuNotFound(eaeuNotFound);

            if (!rfNotFound.isEmpty()) {
                state.setFallbackEaeuRequestId(
                        fsaClient.asyncReaeuFindDoc(buildRequest(rfNotFound)).getRequestId());
                waitForCompletion(state.getFallbackEaeuRequestId());
            }
            if (!eaeuNotFound.isEmpty()) {
                state.setFallbackRfRequestId(
                        fsaClient.asyncRssFindDoc(buildRequest(eaeuNotFound)).getRequestId());
                waitForCompletion(state.getFallbackRfRequestId());
            }

            // РФ-документы, не найденные в РФ, но найденные в ЕАЭС
            if (!rfNotFound.isEmpty()) {
                state.setFallbackEaeuRequestId(
                        fsaClient.asyncReaeuFindDoc(buildRequest(rfNotFound)).getRequestId());
                waitForCompletion(state.getFallbackEaeuRequestId());

                List<CertificateResponseDto> fallbackFound = readResult(state.getFallbackEaeuRequestId());
                // добавляем к eaeuFound (или к отдельному списку fallbackFoundInEaeu)
                state.setEaeuFound(mergeDistinct(state.getEaeuFound(),
                        fallbackFound.stream().map(CertificateResponseDto::getNumberDoc).toList()));
            }

            if (!eaeuNotFound.isEmpty()) {
                state.setFallbackRfRequestId(
                        fsaClient.asyncRssFindDoc(buildRequest(eaeuNotFound)).getRequestId());
                waitForCompletion(state.getFallbackRfRequestId());

                List<CertificateResponseDto> fallbackFound = readResult(state.getFallbackRfRequestId());
                state.setRfFound(mergeDistinct(
                        state.getRfFound(),
                        fallbackFound.stream().map(CertificateResponseDto::getNumberDoc).toList()));
            }

            // 6. Финализируем
            state.setStatus(ImportJobState.JobStatus.SUCCESS);
            jobStore.update(state);
            log.info("Job {} завершён успешно", state.getJobId());

        } catch (Exception e) {
            log.error("Job {} упал", state.getJobId(), e);
            state.setStatus(ImportJobState.JobStatus.FAILED);
            state.setErrorMessage(e.getMessage());
            jobStore.update(state);
        }
    }

    private List<String> mergeDistinct(List<String> a, List<String> b) {
        Set<String> set = new LinkedHashSet<>();
        if (a != null) set.addAll(a);
        if (b != null) set.addAll(b);
        return new ArrayList<>(set);
    }

    // ===== helpers (те же, что были) =====

    private AsyncFindDocRequest buildRequest(List<String> numbers) {
        return AsyncFindDocRequest.builder()
                .items(numbers.stream()
                        .map(num -> new FindDocItem(num, null, null))
                        .toList())
                .build();
    }

    private void waitForCompletion(String requestId) {
        if (requestId == null) return;
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                AsyncStatusResponse status = fsaClient.asyncRequestStatus(requestId);
                String s = status.getStatus() == null ? "" : status.getStatus().trim();
                if ("SUCCESS".equals(s) || "FAILED".equals(s)) return;
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Ошибка опроса {}", requestId, e);
                return;
            }
        }
        log.warn("Таймаут ожидания {}", requestId);
    }

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
            log.error("Ошибка чтения ошибок {}", requestId, e);
            return List.of();
        }
    }

    private List<String> extractNotFound(List<AsyncErrorEntry> errors) {
        List<String> result = new ArrayList<>();
        for (AsyncErrorEntry e : errors) {
            if ("not_found".equals(e.getReason()) && e.getNumberDoc() != null) {
                result.add(e.getNumberDoc());
            }
        }
        return result;
    }

    private List<CertificateResponseDto> readResult(String requestId) {
        if (requestId == null) return List.of();
        try {
            AsyncStatusResponse status = fsaClient.asyncRequestStatus(requestId);
            if (status.getResultAvailable() == null || !status.getResultAvailable()) {
                return List.of();
            }
            byte[] gz = fsaClient.asyncRequestResult(requestId);
            return jsonlGzReader.read(gz, CertificateResponseDto.class);
        } catch (Exception e) {
            log.error("Ошибка чтения результата {}", requestId, e);
            return List.of();
        }
    }
}