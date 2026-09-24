package com.example.fsa_gov.util;

import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonlGzReader {

    private final ObjectMapper objectMapper;

    private static final byte GZIP_MAGIC_1 = (byte) 0x1f;
    private static final byte GZIP_MAGIC_2 = (byte) 0x8b;
    private static final int PREVIEW_LEN = 500;

    public <T> List<T> read(byte[] bytes, Class<T> clazz) throws IOException {
        if (bytes == null || bytes.length == 0) return List.of();

        String content = isGzip(bytes)
                ? gunzip(bytes)
                : new String(bytes, StandardCharsets.UTF_8);

        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') {
            content = content.substring(1);
        }

        List<T> result = new ArrayList<>();
        int lineNo = 0;
        for (String line : content.split("\\R")) {
            lineNo++;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(objectMapper.readValue(trimmed, clazz));
            } catch (Exception e) {
                String preview = trimmed.length() > PREVIEW_LEN
                        ? trimmed.substring(0, PREVIEW_LEN) + "..."
                        : trimmed;
                log.warn("JSONL: не удалось распарсить строку #{} ({}): {} — причина: {}",
                        lineNo, clazz.getSimpleName(), preview, e.getMessage());
            }
        }
        return result;
    }

    private boolean isGzip(byte[] bytes) {
        return bytes.length >= 2
                && bytes[0] == GZIP_MAGIC_1
                && bytes[1] == GZIP_MAGIC_2;
    }

    private String gunzip(byte[] bytes) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(gzis.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}