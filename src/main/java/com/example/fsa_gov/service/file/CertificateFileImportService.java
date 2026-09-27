package com.example.fsa_gov.service.file;

import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.dto.file.FileImportResult;
import com.example.fsa_gov.parser.CertificateNumberNormalizer;
import com.example.fsa_gov.service.CertificateImportService;
import com.example.fsa_gov.service.job.ImportJobState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Оркестратор: читает файл, режет на батчи, гоняет через существующий
 * CertificateImportService.runSync, аккумулирует результаты.
 *
 * Опционально нормализует номера перед импортом (флаг `normalize`).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateFileImportService {

    private final FileLineReader fileLineReader;
    private final CertificateImportService importService;
    private final FileResultWriter resultWriter;
    private final CertificateNumberNormalizer normalizer;

    public FileImportResult importFromFile(String filePath,
                                           int columnIndex,
                                           int batchSize,
                                           String outputDir,
                                           boolean normalize) throws IOException {

        // 1. Читаем строки
        List<String> allLines = fileLineReader.readLines(filePath, columnIndex);
        if (allLines.isEmpty()) {
            throw new IOException("Файл не содержит строк: " + filePath);
        }

        // 2. Нормализация (если включена)
        Map<String, String> normalizedMap = new LinkedHashMap<>();  // normalized → original
        List<String> linesForImport = new ArrayList<>(allLines.size());

        for (String original : allLines) {
            if (normalize) {
                String norm = normalizer.normalize(original);
                linesForImport.add(norm);
                if (norm != null && !norm.equals(original)) {
                    normalizedMap.putIfAbsent(norm, original);
                }
            } else {
                linesForImport.add(original);
            }
        }

        FileImportResult result = new FileImportResult();
        result.setTotalLines(allLines.size());
        result.setNormalizeEnabled(normalize);
        result.setTotalNormalized(normalizedMap.size());

        // 3. Итерации по batchSize
        int batches = (linesForImport.size() + batchSize - 1) / batchSize;
        result.setTotalBatches(batches);
        log.info("Файл {}: всего строк={}, батчей={} по {}, normalize={}",
                filePath, allLines.size(), batches, batchSize, normalize);

        for (int i = 0; i < batches; i++) {
            int from = i * batchSize;
            int to = Math.min(from + batchSize, linesForImport.size());
            List<String> batch = linesForImport.subList(from, to);

            log.info("Батч {}/{}: строк {}-{}", i + 1, batches, from, to);
            ImportJobState batchResult = importService.runSync(batch);

            // 4. Проставляем sourceNumber и normalized в найденных DTO
            if (normalize) {
                markNormalized(batchResult.getRfFound(),   normalizedMap);
                markNormalized(batchResult.getEaeuFound(), normalizedMap);
            }

            // 5. Аккумулируем found — и РФ, и ЕАЭС в один список
            mergeFound(result.getFound(), batchResult.getRfFound());
            mergeFound(result.getFound(), batchResult.getEaeuFound());

            // 6. Аккумулируем notFound — тоже в один список
            addAllDistinct(result.getNotFound(), batchResult.getRfNotFound());
            addAllDistinct(result.getNotFound(), batchResult.getEaeuNotFound());
            addAllDistinct(result.getInvalidEntries(), batchResult.getInvalidEntries());

            log.info("После батча {}: found={}, notFound={}, мусор={}",
                    i + 1,
                    result.getFoundCount(),
                    result.getNotFoundCount(),
                    result.getInvalidCount());
        }

        // 7. Пишем файлы
        String foundFile = resultWriter.writeDtoList(
                outputDir, "result-found.json", result.getFound());
        String notFoundFile = resultWriter.writeNotFound(
                outputDir, "result-not-found.json",
                result.getNotFound(), result.getInvalidEntries());

        result.setFoundFile(foundFile);
        result.setNotFoundFile(notFoundFile);

        log.info("Импорт завершён: found={}, notFound={}, мусор={}, normalize={}",
                result.getFoundCount(), result.getNotFoundCount(),
                result.getInvalidCount(), normalize);

        return result;
    }

    /* ================= helpers ================= */

    /**
     * Проставляет sourceNumber и normalized в найденных DTO.
     */
    private void markNormalized(List<CertificateResponseDto> list,
                                Map<String, String> normalizedMap) {
        if (list == null) return;
        for (CertificateResponseDto dto : list) {
            if (dto == null || dto.getNumberDoc() == null) continue;

            String source = normalizedMap.get(dto.getNumberDoc());
            if (source != null) {
                dto.setSourceNumber(source);
                dto.setNormalized(true);
            } else {
                if (dto.getSourceNumber() == null) {
                    dto.setSourceNumber(dto.getNumberDoc());
                }
                dto.setNormalized(false);
            }
        }
    }

    private void mergeFound(List<CertificateResponseDto> target,
                            List<CertificateResponseDto> source) {
        if (source == null) return;
        Map<String, CertificateResponseDto> map = new LinkedHashMap<>();
        for (CertificateResponseDto d : target) {
            if (d != null && d.getNumberDoc() != null) map.putIfAbsent(d.getNumberDoc(), d);
        }
        for (CertificateResponseDto d : source) {
            if (d != null && d.getNumberDoc() != null) map.putIfAbsent(d.getNumberDoc(), d);
        }
        target.clear();
        target.addAll(map.values());
    }

    private void addAllDistinct(List<String> target, List<String> source) {
        if (source == null) return;
        for (String s : source) {
            if (!target.contains(s)) target.add(s);
        }
    }
}