package com.example.fsa_gov.service.job;

import com.example.fsa_gov.client.FsaClient;
import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.dto.async.AsyncErrorEntry;
import com.example.fsa_gov.dto.async.AsyncFindDocRequest;
import com.example.fsa_gov.dto.async.AsyncStatusResponse;
import com.example.fsa_gov.dto.async.FindDocItem;
import com.example.fsa_gov.parser.dto.ParseResult;
import com.example.fsa_gov.util.JsonlGzReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportJobRunner {

    private final FsaClient fsaClient;
    private final JsonlGzReader jsonlGzReader;
    private final ImportJobStore jobStore;

    private static final long POLL_TIMEOUT_MS = 120_000L;
    private static final long POLL_INTERVAL_MS = 2_000L;

    @Async("importExecutor")
    public void run(ImportJobState state, ParseResult parsed) {
        process(state, parsed);
    }

    public void runSync(ImportJobState state, ParseResult parsed) {
        process(state, parsed);
    }

    private void process(ImportJobState state, ParseResult parsed) {
        try {
            state.setStatus(ImportJobState.JobStatus.RUNNING);
            state.setInvalidEntries(parsed.getInvalidEntries());
            jobStore.update(state);

            if (parsed.getRfCertificates() != null && !parsed.getRfCertificates().isEmpty()) {
                state.setRfRequestId(startAsync(state, parsed.getRfCertificates(), true));
            }
            if (parsed.getEaeuCertificates() != null && !parsed.getEaeuCertificates().isEmpty()) {
                state.setEaeuRequestId(startAsync(state, parsed.getEaeuCertificates(), false));
            }
            jobStore.update(state);

            waitForCompletion(state.getRfRequestId());
            waitForCompletion(state.getEaeuRequestId());

            List<CertificateResponseDto> rfFound = readResult(state.getRfRequestId());
            List<CertificateResponseDto> eaeuFound = readResult(state.getEaeuRequestId());

            List<AsyncErrorEntry> rfErrors = readErrors(state.getRfRequestId());
            List<AsyncErrorEntry> eaeuErrors = readErrors(state.getEaeuRequestId());

            state.setRfFound(rfFound);
            state.setEaeuFound(eaeuFound);

            log.info("Job {}: ошибок РФ={}, ЕАЭС={}", state.getJobId(), rfErrors.size(), eaeuErrors.size());
            for (AsyncErrorEntry e : rfErrors) {
                log.info("  RF  err: numberDoc=[{}], reason={}, matchCount={}",
                        e.getNumberDoc(), e.getReason(), e.getMatchCount());
            }
            for (AsyncErrorEntry e : eaeuErrors) {
                log.info("  EAEU err: numberDoc=[{}], reason={}, matchCount={}",
                        e.getNumberDoc(), e.getReason(), e.getMatchCount());
            }

            List<String> rfNotFound = extractNotFound(rfErrors);
            List<String> eaeuNotFound = extractNotFound(eaeuErrors);
            state.setRfNotFound(rfNotFound);
            state.setEaeuNotFound(eaeuNotFound);

            if (!rfNotFound.isEmpty()) {
                log.info("Job {}: fallback РФ→ЕАЭС для {} номеров", state.getJobId(), rfNotFound.size());
                String fbId = startAsync(state, rfNotFound, false);
                state.setFallbackEaeuRequestId(fbId);
                waitForCompletion(fbId);
                state.setEaeuFound(mergeDistinctDtos(state.getEaeuFound(), readResult(fbId)));
            }

            if (!eaeuNotFound.isEmpty()) {
                log.info("Job {}: fallback ЕАЭС→РФ для {} номеров", state.getJobId(), eaeuNotFound.size());
                String fbId = startAsync(state, eaeuNotFound, true);
                state.setFallbackRfRequestId(fbId);
                waitForCompletion(fbId);
                state.setRfFound(mergeDistinctDtos(state.getRfFound(), readResult(fbId)));
            }

            state.setStatus(ImportJobState.JobStatus.SUCCESS);
            jobStore.update(state);
            log.info("Job {} завершён. РФ={}, ЕАЭС={}, не найдено РФ={}, не найдено ЕАЭС={}",
                    state.getJobId(),
                    size(state.getRfFound()), size(state.getEaeuFound()),
                    size(state.getRfNotFound()), size(state.getEaeuNotFound()));

        } catch (Exception e) {
            log.error("Job {} упал", state.getJobId(), e);
            state.setStatus(ImportJobState.JobStatus.FAILED);
            state.setErrorMessage(e.getMessage());
            jobStore.update(state);
        }
    }

    /* ======================= helpers ======================= */

    private String startAsync(ImportJobState state, List<String> numbers, boolean rf) {
        AsyncFindDocRequest request = buildRequest(numbers);
        var response = rf
                ? fsaClient.asyncRssFindDoc(request)
                : fsaClient.asyncReaeuFindDoc(request);
        if (response == null || response.getRequestId() == null) {
            throw new IllegalStateException("API ФСА не вернул requestId для " + (rf ? "РФ" : "ЕАЭС"));
        }
        return response.getRequestId();
    }

    private AsyncFindDocRequest buildRequest(List<String> numbers) {
        List<FindDocItem> items = numbers.stream()
                .map(num -> new FindDocItem(num, null, null))
                .toList();
        return AsyncFindDocRequest.builder().items(items).build();
    }

    private void waitForCompletion(String requestId) {
        if (requestId == null) return;
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            try {
                AsyncStatusResponse status = fsaClient.asyncRequestStatus(requestId);
                String s = status == null || status.getStatus() == null ? "" : status.getStatus().trim();
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
            if (status == null || !Boolean.TRUE.equals(status.getErrorsAvailable())) {
                return List.of();
            }
            byte[] gz = fsaClient.asyncRequestErrors(requestId);
            return jsonlGzReader.read(gz, AsyncErrorEntry.class);
        } catch (Exception e) {
            log.error("Ошибка чтения ошибок {}", requestId, e);
            return List.of();
        }
    }

    private List<CertificateResponseDto> readResult(String requestId) {
        if (requestId == null) return List.of();
        try {
            AsyncStatusResponse status = fsaClient.asyncRequestStatus(requestId);
            if (status == null || !Boolean.TRUE.equals(status.getResultAvailable())) {
                return List.of();
            }
            byte[] gz = fsaClient.asyncRequestResult(requestId);
            return jsonlGzReader.read(gz, CertificateResponseDto.class);
        } catch (Exception e) {
            log.error("Ошибка чтения результата {}", requestId, e);
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

    /** Мерж двух списков DTO с дедупликацией по numberDoc. */
    private List<CertificateResponseDto> mergeDistinctDtos(
            List<CertificateResponseDto> a,
            List<CertificateResponseDto> b) {
        Map<String, CertificateResponseDto> map = new LinkedHashMap<>();
        if (a != null) for (CertificateResponseDto d : a) {
            if (d != null && d.getNumberDoc() != null) map.putIfAbsent(d.getNumberDoc(), d);
        }
        if (b != null) for (CertificateResponseDto d : b) {
            if (d != null && d.getNumberDoc() != null) map.putIfAbsent(d.getNumberDoc(), d);
        }
        return new ArrayList<>(map.values());
    }

    private int size(List<?> l) {
        return l == null ? 0 : l.size();
    }
}