package com.example.fsa_gov.service;

import com.example.fsa_gov.parser.CertificateNumberNormalizer;
import com.example.fsa_gov.service.job.ImportJobState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис диагностической проверки нормализации номеров.
 *
 * Прогоняет один и тот же список номеров дважды:
 *   1) как есть;
 *   2) после нормализации через {@link CertificateNumberNormalizer}.
 * Сравнивает результаты и возвращает сводку.
 *
 * НЕ участвует в основном пайплайне импорта.
 * Используется вручную через /api/v1/certificates/import/normalize-check.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizeCheckService {

    private final CertificateNumberNormalizer normalizer;
    private final CertificateImportService importService;

    /**
     * Прогоняет список «как есть» и «нормализованный», возвращает сводку.
     */
    public Map<String, Object> check(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return Map.of(
                    "original",   Map.of(),
                    "normalized", Map.of(),
                    "beforeFound", 0,
                    "afterFound",  0
            );
        }

        // 1. Нормализуем
        List<String> normalized = new ArrayList<>(lines.size());
        List<String> changes = new ArrayList<>();

        for (String line : lines) {
            String n = normalizer.normalize(line);
            normalized.add(n);
            if (n != null && !n.equals(line)) {
                changes.add(line + " → " + n);
            }
        }

        // 2. Прогоняем оба списка через существующий импорт
        ImportJobState originalResult   = importService.runSync(lines);
        ImportJobState normalizedResult = importService.runSync(normalized);

        // 3. Собираем сводку
        Map<String, Object> originalSummary   = summarize(originalResult);
        Map<String, Object> normalizedSummary = summarize(normalizedResult);

        int beforeFound = size(originalResult.getRfFound())
                + size(originalResult.getEaeuFound());
        int afterFound  = size(normalizedResult.getRfFound())
                + size(normalizedResult.getEaeuFound());

        log.info("normalize-check: вход={}, изменено={}, найдено до={}, после={}",
                lines.size(), changes.size(), beforeFound, afterFound);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLines",      lines.size());
        result.put("totalNormalized", changes.size());
        result.put("original",        originalSummary);
        result.put("normalized",      normalizedSummary);
        result.put("beforeFound",     beforeFound);
        result.put("afterFound",      afterFound);
        result.put("changes",         changes);

        return result;
    }

    /* ===================== helpers ===================== */

    /**
     * Превращает {@link ImportJobState} в краткую сводку:
     * сколько найдено, сколько не найдено, какие именно номера.
     */
    private Map<String, Object> summarize(ImportJobState state) {
        Map<String, Object> s = new LinkedHashMap<>();

        s.put("jobId",            state.getJobId());
        s.put("status",           state.getStatus() != null ? state.getStatus().name() : null);
        s.put("errorMessage",     state.getErrorMessage());

        s.put("rfFoundCount",     size(state.getRfFound()));
        s.put("eaeuFoundCount",   size(state.getEaeuFound()));
        s.put("rfNotFoundCount",  size(state.getRfNotFound()));
        s.put("eaeuNotFoundCount", size(state.getEaeuNotFound()));
        s.put("invalidCount",     size(state.getInvalidEntries()));

        s.put("rfFoundNumbers",     extractNumbers(state.getRfFound()));
        s.put("eaeuFoundNumbers",   extractNumbers(state.getEaeuFound()));
        s.put("rfNotFound",         state.getRfNotFound());
        s.put("eaeuNotFound",       state.getEaeuNotFound());
        s.put("invalidEntries",     state.getInvalidEntries());

        return s;
    }

    /**
     * Достаёт список numberDoc из списка DTO.
     */
    private List<String> extractNumbers(List<com.example.fsa_gov.dto.CertificateResponseDto> list) {
        if (list == null) return List.of();
        List<String> result = new ArrayList<>(list.size());
        for (var dto : list) {
            if (dto != null && dto.getNumberDoc() != null) {
                result.add(dto.getNumberDoc());
            }
        }
        return result;
    }

    private int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}