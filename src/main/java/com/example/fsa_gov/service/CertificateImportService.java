package com.example.fsa_gov.service;

import com.example.fsa_gov.client.FsaClient;
import com.example.fsa_gov.dto.async.AsyncFindDocRequest;
import com.example.fsa_gov.dto.async.AsyncFindDocResponse;
import com.example.fsa_gov.dto.async.FindDocItem;
import com.example.fsa_gov.parser.CertificateListParser;
import com.example.fsa_gov.parser.dto.ParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис импорта и проверки списка сертификатов.
 *
 * <p>Разбивает входной список на РФ / ЕАЭС / некорректные,
 * затем запускает асинхронную проверку через API ФСА.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateImportService {

    private final CertificateListParser parser;
    private final FsaClient fsaClient;

    /**
     * Парсит список строк и классифицирует их.
     *
     * @param lines список строк (номеров сертификатов)
     * @return {@link ParseResult} с тремя списками
     */
    public ParseResult parseLines(List<String> lines) {
        ParseResult result = parser.parse(lines);

        log.info("Парсинг завершён: РФ={}, ЕАЭС={}, некорректных={}",
                result.getRfCertificates().size(),
                result.getEaeuCertificates().size(),
                result.getInvalidEntries().size());

        result.getInvalidEntries().forEach(line ->
                log.warn("Некорректная строка: '{}'", line)
        );

        return result;
    }

    /**
     * Запускает асинхронную проверку распарсенных сертификатов.
     *
     * <p>Для РФ вызывается {@code asyncRssFindDoc},
     * для ЕАЭС — {@code asyncReaeuFindDoc}.
     *
     * @param result результат парсинга
     * @return {@link ImportResult} с requestId для каждого реестра
     */
    public ImportResult startImport(ParseResult result) {
        ImportResult importResult = new ImportResult();

        // РФ → asyncRssFindDoc
        if (!result.getRfCertificates().isEmpty()) {
            try {
                AsyncFindDocRequest req = AsyncFindDocRequest.builder()
                        .items(result.getRfCertificates().stream()
                                .map(num -> new FindDocItem(num, null, null))
                                .toList())
                        .build();

                AsyncFindDocResponse resp = fsaClient.asyncRssFindDoc(req);
                importResult.setRfRequestId(resp.getRequestId());
                log.info("Запущен поиск СС РФ: {} документов, requestId={}",
                        result.getRfCertificates().size(), resp.getRequestId());
            } catch (Exception e) {
                log.error("Ошибка запуска asyncRssFindDoc", e);
                importResult.setRfError(e.getMessage());
            }
        }

        // ЕАЭС → asyncReaeuFindDoc
        if (!result.getEaeuCertificates().isEmpty()) {
            try {
                AsyncFindDocRequest req = AsyncFindDocRequest.builder()
                        .items(result.getEaeuCertificates().stream()
                                .map(num -> new FindDocItem(num, null, null))
                                .toList())
                        .build();

                AsyncFindDocResponse resp = fsaClient.asyncReaeuFindDoc(req);
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
}