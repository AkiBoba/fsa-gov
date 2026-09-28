package com.example.fsa_gov.controller;

import com.example.fsa_gov.dto.file.FileImportResult;
import com.example.fsa_gov.service.file.CertificateFileImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates/import/file")
@Tag(name = "Импорт из файла", description = "Пакетная проверка списка номеров из xlsx/txt")
public class CertificateFileImportController {

    private final CertificateFileImportService fileImportService;

    @PostMapping(value = "/run", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Запустить импорт из файла",
            description = """
                    Принимает файл (.xlsx — колонка `columnIndex`, или .txt) через multipart/form-data (поле `file`),
                    режет на батчи по `batchSize`,
                    гоняет через ФСА (РФ + ЕАЭС + fallback),
                    пишет 3 JSON-файла в `outputDir` и возвращает сводку.
                    """
    )
    public ResponseEntity<FileImportResult> run(
            @Parameter(
                    description = "Файл со списком номеров (.xlsx или .txt)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Индекс колонки для .xlsx (по умолчанию 0)")
            @RequestParam(value = "columnIndex", required = false, defaultValue = "0") Integer columnIndex,

            @Parameter(description = "Размер батча (по умолчанию 1000)")
            @RequestParam(value = "batchSize", required = false, defaultValue = "1000") Integer batchSize,

            @Parameter(description = "Каталог для сохранения результатов", example = "D:/files/output")
            @RequestParam(value = "outputDir", required = false, defaultValue = "D:/files/output") String outputDir,

            @Parameter(description = "Нормализовать номера перед импортом")
            @RequestParam(value = "normalize", required = false, defaultValue = "false") Boolean normalize
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран или пуст");
        }

        log.info("Запуск импорта из файла: name={}, size={}, column={}, batch={}, output={}, normalize={}",
                file.getOriginalFilename(), file.getSize(), columnIndex,
                batchSize, outputDir, normalize);

        // Сохраняем загруженный файл во временный файл, чтобы передать путь в существующий сервис
        String suffix = extractSuffix(file.getOriginalFilename());
        Path tempFile = Files.createTempFile("import-", suffix);
        try {
            file.transferTo(tempFile);

            FileImportResult result = fileImportService.importFromFile(
                    tempFile.toString(),
                    columnIndex,
                    batchSize,
                    outputDir,
                    Boolean.TRUE.equals(normalize)
            );

            return ResponseEntity.ok(result);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static String extractSuffix(String originalName) {
        if (originalName == null) {
            return ".tmp";
        }
        int dot = originalName.lastIndexOf('.');
        return (dot >= 0) ? originalName.substring(dot) : ".tmp";
    }
}