package com.example.fsa_gov.service;

import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.parser.CertificateNumberNormalizer;
import com.example.fsa_gov.service.file.FileResultWriter;
import com.example.fsa_gov.service.file.NotFoundFileReader;
import com.example.fsa_gov.service.job.ImportJobState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Диагностический сервис: принимает result-not-found.json,
 * нормализует номера, прогоняет через существующий флоу,
 * проставляет sourceNumber в найденных DTO и сохраняет результаты в файлы.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizeCheckService {

    private final CertificateNumberNormalizer normalizer;
    private final CertificateImportService importService;
    private final NotFoundFileReader notFoundFileReader;
    private final FileResultWriter fileResultWriter;

    /**
     * Основная логика:
     *  1) читаем файл → List<String>;
     *  2) строим мапу original → normalized;
     *  3) прогоняем original (как есть) и normalized через runSync;
     *  4) проставляем sourceNumber в normalized-результатах;
     *  5) сохраняем result-rf-found.json и result-eaeu-found.json;
     *  6) возвращаем сводку.
     */
    public Map<String, Object> check(MultipartFile file, String outputDir) throws IOException {

        // 1. Читаем файл → список номеров
        List<String> originalLines = notFoundFileReader.read(file);

        if (originalLines.isEmpty()) {
            return emptyResult();
        }

        // 2. Строим мапу original → normalized (первое совпадение)
        //    LinkedHashMap — сохраняет порядок, удобно для отладки
        Map<String, String> normalizedMap = new LinkedHashMap<>();
        List<String> normalizedLines = new ArrayList<>(originalLines.size());

        for (String original : originalLines) {
            String norm = normalizer.normalize(original);
            normalizedLines.add(norm);
            // Первое совпадение: putIfAbsent, чтобы не перезаписывать
            normalizedMap.putIfAbsent(norm, original);
        }

        // 3. Прогоняем оба списка
        ImportJobState originalResult   = importService.runSync(originalLines);
        ImportJobState normalizedResult = importService.runSync(normalizedLines);

        // 4. Проставляем sourceNumber в найденных DTO нормализованного прогона
        fillSourceNumber(normalizedResult.getRfFound(),   normalizedMap);
        fillSourceNumber(normalizedResult.getEaeuFound(), normalizedMap);

        // 5. Сохраняем результаты нормализованного прогона в файлы
        String rfFile   = fileResultWriter.writeDtoList(
                outputDir, "result-rf-found.json",   normalizedResult.getRfFound());
        String eaeuFile = fileResultWriter.writeDtoList(
                outputDir, "result-eaeu-found.json", normalizedResult.getEaeuFound());

        log.info("normalize-check: сохранены {} и {}", rfFile, eaeuFile);

        // 6. Собираем сводку
        Map<String, Object> originalSummary   = summarize(originalResult,   false);
        Map<String, Object> normalizedSummary = summarize(normalizedResult, true);

        int beforeFound = size(originalResult.getRfFound())
                + size(originalResult.getEaeuFound());
        int afterFound  = size(normalizedResult.getRfFound())
                + size(normalizedResult.getEaeuFound());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLines",       originalLines.size());
        result.put("totalNormalized",  countChanged(normalizedMap));
        result.put("beforeFound",      beforeFound);
        result.put("afterFound",       afterFound);
        result.put("rfFoundFile",      rfFile);
        result.put("eaeuFoundFile",    eaeuFile);
        result.put("original",         originalSummary);
        result.put("normalized",       normalizedSummary);
        result.put("normalizedMap",    normalizedMap);

        log.info("normalize-check: вход={}, изменено={}, найдено до={}, после={}",
                originalLines.size(), countChanged(normalizedMap), beforeFound, afterFound);

        return result;
    }

    /* ===================== helpers ===================== */

    /**
     * Проставляет sourceNumber в DTO по мапе normalized → original.
     * Если в мапе нет — не трогает (значение проставит CertificateImportService).
     */
    private void fillSourceNumber(List<CertificateResponseDto> list,
                                  Map<String, String> normalizedMap) {
        if (list == null) return;
        for (CertificateResponseDto dto : list) {
            if (dto == null || dto.getNumberDoc() == null) continue;
            String source = normalizedMap.get(dto.getNumberDoc());
            if (source != null) {
                dto.setSourceNumber(source);
            }
        }
    }

    /**
     * Сводка по ImportJobState.
     * withSourceNumber = true — rfFound/eaeuFound возвращаем как DTO,
     *                    false — как список строк numberDoc.
     */
    private Map<String, Object> summarize(ImportJobState state, boolean withDtos) {
        Map<String, Object> s = new LinkedHashMap<>();

        s.put("jobId",             state.getJobId());
        s.put("status",            state.getStatus() != null ? state.getStatus().name() : null);
        s.put("errorMessage",      state.getErrorMessage());

        s.put("rfFoundCount",      size(state.getRfFound()));
        s.put("eaeuFoundCount",    size(state.getEaeuFound()));
        s.put("rfNotFoundCount",   size(state.getRfNotFound()));
        s.put("eaeuNotFoundCount", size(state.getEaeuNotFound()));
        s.put("invalidCount",      size(state.getInvalidEntries()));

        if (withDtos) {
            // DTO целиком, с sourceNumber внутри
            s.put("rfFound",   state.getRfFound());
            s.put("eaeuFound", state.getEaeuFound());
        } else {
            // только строки (для оригинального прогона)
            s.put("rfFoundNumbers",   extractNumbers(state.getRfFound()));
            s.put("eaeuFoundNumbers", extractNumbers(state.getEaeuFound()));
        }

        s.put("rfNotFound",    state.getRfNotFound());
        s.put("eaeuNotFound",  state.getEaeuNotFound());
        s.put("invalidEntries", state.getInvalidEntries());

        return s;
    }

    private List<String> extractNumbers(List<CertificateResponseDto> list) {
        if (list == null) return List.of();
        List<String> result = new ArrayList<>(list.size());
        for (var dto : list) {
            if (dto != null && dto.getNumberDoc() != null) {
                result.add(dto.getNumberDoc());
            }
        }
        return result;
    }

    /** Сколько записей в мапе реально изменилось (original != normalized). */
    private int countChanged(Map<String, String> map) {
        int count = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (!e.getKey().equals(e.getValue())) count++;
        }
        return count;
    }

    private int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalLines",      0);
        m.put("totalNormalized", 0);
        m.put("beforeFound",     0);
        m.put("afterFound",      0);
        m.put("original",        Map.of());
        m.put("normalized",      Map.of());
        m.put("normalizedMap",   Map.of());
        return m;
    }
}