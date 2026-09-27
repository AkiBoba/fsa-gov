package com.example.fsa_gov.service.file;

import com.example.fsa_gov.dto.notFoundToExcel.NotFoundFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Читает result-not-found.json и разворачивает его в плоский список номеров.
 *
 * Берёт все три списка: rfNotFound + eaeuNotFound + invalidEntries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotFoundFileReader {

    private final ObjectMapper objectMapper;

    public List<String> read(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }

        NotFoundFileDto dto = objectMapper.readValue(
                file.getInputStream(), NotFoundFileDto.class);

        List<String> result = new ArrayList<>();
        if (dto.getRfNotFound()    != null) result.addAll(dto.getRfNotFound());
        if (dto.getEaeuNotFound()  != null) result.addAll(dto.getEaeuNotFound());
        if (dto.getInvalidEntries()!= null) result.addAll(dto.getInvalidEntries());

        log.info("NotFoundFileReader: прочитано {} номеров из {}",
                result.size(), file.getOriginalFilename());
        return result;
    }
}