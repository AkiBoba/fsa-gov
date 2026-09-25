package com.example.fsa_gov.controller;

import com.example.fsa_gov.dto.file.FileImportRequest;
import com.example.fsa_gov.dto.file.FileImportResult;
import com.example.fsa_gov.service.file.CertificateFileImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * Импорт из файла: путь к файлу → батчи → ФСА → JSON-файлы результатов.
 * Не меняет существующие контроллеры.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates/import/file")
@Tag(name = "Импорт из файла", description = "Пакетная проверка списка номеров из xlsx/txt")
public class CertificateFileImportController {

    private final CertificateFileImportService fileImportService;

    @PostMapping("/run")
    @Operation(
            summary = "Запустить импорт из файла",
            description = """
                    Читает файл (.xlsx — колонка `columnIndex`, или .txt),
                    режет на батчи по `batchSize`,
                    гоняет через ФСА (РФ + ЕАЭС + fallback),
                    пишет 3 JSON-файла в `outputDir` и возвращает сводку.
                    """
    )
    public ResponseEntity<FileImportResult> run(@Valid @RequestBody FileImportRequest request)
            throws IOException {

        log.info("Запуск импорта из файла: path={}, column={}, batch={}, output={}",
                request.getFilePath(), request.getColumnIndex(),
                request.getBatchSize(), request.getOutputDir());

        FileImportResult result = fileImportService.importFromFile(
                request.getFilePath(),
                request.getColumnIndex() != null ? request.getColumnIndex() : 0,
                request.getBatchSize() != null ? request.getBatchSize() : 1000,
                request.getOutputDir() != null ? request.getOutputDir() : "./output"
        );

        return ResponseEntity.ok(result);
    }
}