package com.example.fsa_gov.service.file;

import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.dto.file.FileImportResult;
import com.example.fsa_gov.service.CertificateImportService;
import com.example.fsa_gov.service.job.ImportJobState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Оркестратор: читает файл, режет на батчи, гоняет через существующий
 * CertificateImportService.runSync, аккумулирует результаты.
 *
 * НЕ меняет существующий код. Использует только его публичные методы.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateFileImportService {

    private final FileLineReader fileLineReader;
    private final CertificateImportService importService;
    private final FileResultWriter resultWriter;

    public FileImportResult importFromFile(String filePath,
                                           int columnIndex,
                                           int batchSize,
                                           String outputDir) throws IOException {

        // 1. Читаем строки
        List<String> allLines = fileLineReader.readLines(filePath, columnIndex);
        if (allLines.isEmpty()) {
            throw new IOException("Файл не содержит строк: " + filePath);
        }

        FileImportResult result = new FileImportResult();
        result.setTotalLines(allLines.size());

        // 2. Итерации по batchSize
        int batches = (allLines.size() + batchSize - 1) / batchSize;
        result.setTotalBatches(batches);
        log.info("Файл {}: всего строк={}, батчей={} по {}",
                filePath, allLines.size(), batches, batchSize);

        for (int i = 0; i < batches; i++) {
            int from = i * batchSize;
            int to = Math.min(from + batchSize, allLines.size());
            List<String> batch = allLines.subList(from, to);

            log.info("Батч {}/{}: строк {}-{}", i + 1, batches, from, to);
            ImportJobState batchResult = importService.runSync(batch);

            // 3. Аккумулируем
            mergeFound(result.getRfFound(), batchResult.getRfFound());
            mergeFound(result.getEaeuFound(), batchResult.getEaeuFound());

            addAllDistinct(result.getRfNotFound(),    batchResult.getRfNotFound());
            addAllDistinct(result.getEaeuNotFound(),  batchResult.getEaeuNotFound());
            addAllDistinct(result.getInvalidEntries(), batchResult.getInvalidEntries());

            log.info("После батча {}: РФ={}, ЕАЭС={}, не найдено РФ={}, не найдено ЕАЭС={}, мусор={}",
                    i + 1,
                    result.getRfFoundCount(), result.getEaeuFoundCount(),
                    result.getRfNotFoundCount(), result.getEaeuNotFoundCount(),
                    result.getInvalidCount());
        }

        // 4. Пишем файлы
        String rfFile      = resultWriter.writeDtoList(outputDir, "result-rf-found.json",
                result.getRfFound());
        String eaeuFile    = resultWriter.writeDtoList(outputDir, "result-eaeu-found.json",
                result.getEaeuFound());
        String notFoundFile = resultWriter.writeNotFound(outputDir, "result-not-found.json",
                result.getRfNotFound(), result.getEaeuNotFound(), result.getInvalidEntries());

        result.setRfFoundFile(rfFile);
        result.setEaeuFoundFile(eaeuFile);
        result.setNotFoundFile(notFoundFile);

        log.info("Импорт завершён: РФ={}, ЕАЭС={}, не найдено РФ={}, не найдено ЕАЭС={}, мусор={}",
                result.getRfFoundCount(), result.getEaeuFoundCount(),
                result.getRfNotFoundCount(), result.getEaeuNotFoundCount(),
                result.getInvalidCount());

        return result;
    }

    /* ================= helpers ================= */

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