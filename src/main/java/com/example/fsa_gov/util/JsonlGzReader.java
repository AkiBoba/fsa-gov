package com.example.fsa_gov.util;

import tools.jackson.databind.ObjectMapper;   // вместо com.fasterxml.jackson
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Component
@RequiredArgsConstructor
public class JsonlGzReader {

    private final ObjectMapper objectMapper;

    /**
     * Читает GZIP-архив с JSONL внутри и возвращает список объектов.
     */
    public <T> List<T> read(byte[] gzBytes, Class<T> clazz) throws IOException {
        List<T> result = new ArrayList<>();
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(gzBytes))) {
            String content = new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
            for (String line : content.split("\\R")) {
                if (!line.isBlank()) {
                    result.add(objectMapper.readValue(line, clazz));
                }
            }
        }
        return result;
    }
}