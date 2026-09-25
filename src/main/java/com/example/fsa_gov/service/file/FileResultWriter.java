package com.example.fsa_gov.service.file;

import com.example.fsa_gov.dto.CertificateResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Пишет результаты импорта в JSON-файлы.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileResultWriter {

    private final ObjectMapper objectMapper;

    /** Список DTO → JSON-массив в файл. Возвращает путь. */
    public String writeDtoList(String outputDir, String fileName,
                               List<CertificateResponseDto> list) throws IOException {
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);
        Path file = dir.resolve(fileName);

        String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(list);
        Files.writeString(file, json, StandardCharsets.UTF_8);

        log.info("Записан файл {}: {} записей", file.toAbsolutePath(), list.size());
        return file.toAbsolutePath().toString();
    }

    /** Объединённый файл «не найдено + мусор». */
    public String writeNotFound(String outputDir, String fileName,
                                List<String> rfNotFound,
                                List<String> eaeuNotFound,
                                List<String> invalidEntries) throws IOException {
        Path dir = Path.of(outputDir);
        Files.createDirectories(dir);
        Path file = dir.resolve(fileName);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rfNotFoundCount",   rfNotFound.size());
        payload.put("eaeuNotFoundCount", eaeuNotFound.size());
        payload.put("invalidCount",      invalidEntries.size());
        payload.put("rfNotFound",        rfNotFound);
        payload.put("eaeuNotFound",      eaeuNotFound);
        payload.put("invalidEntries",    invalidEntries);

        String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(payload);
        Files.writeString(file, json, StandardCharsets.UTF_8);

        log.info("Записан файл {}: РФ-не найдено={}, ЕАЭС-не найдено={}, мусор={}",
                file.toAbsolutePath(),
                rfNotFound.size(), eaeuNotFound.size(), invalidEntries.size());
        return file.toAbsolutePath().toString();
    }
}