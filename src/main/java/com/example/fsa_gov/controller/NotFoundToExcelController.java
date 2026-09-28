package com.example.fsa_gov.controller;

import com.example.fsa_gov.dto.notFoundToExcel.NotFoundExportResult;
import com.example.fsa_gov.service.notFoundToExcel.NotFoundToExcelService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates/import/file")
@Tag(name = "Импорт из файла")
public class NotFoundToExcelController {

    private final NotFoundToExcelService service;

    @PostMapping(
            value = "/not-found-to-excel",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Экспорт result-not-found.json в Excel",
            description = """
                    Принимает result-not-found.json (multipart/form-data, поле `file`),
                    классифицирует номера на «возможно валидные» и «мусор»,
                    пишет XLSX с листами:
                      - Возможно валидные
                      - Мусор
                      - Все подряд (если `splitSheets = true`)
                    """
    )
    public ResponseEntity<NotFoundExportResult> export(
            @Parameter(
                    description = "JSON-файл result-not-found.json",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "Путь для сохранения XLSX",
                    example = "D:/files/output/result-not-found.xlsx")
            @RequestParam(value = "excelFilePath", required = false,
                    defaultValue = "D:/files/output/result-not-found.xlsx") String excelFilePath,

            @Parameter(description = "Разбивать на отдельные листы")
            @RequestParam(value = "splitSheets", required = false,
                    defaultValue = "true") Boolean splitSheets
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран или пуст");
        }

        log.info("not-found-to-excel: file={}, size={}, excel={}, splitSheets={}",
                file.getOriginalFilename(), file.getSize(),
                excelFilePath, splitSheets);

        NotFoundExportResult result = service.export(
                file, excelFilePath, Boolean.TRUE.equals(splitSheets));

        return ResponseEntity.ok(result);
    }
}