package com.example.fsa_gov.controller;

import com.example.fsa_gov.dto.notFoundToExcel.NotFoundExportResult;
import com.example.fsa_gov.dto.notFoundToExcel.NotFoundToExcelRequest;
import com.example.fsa_gov.service.notFoundToExcel.NotFoundToExcelService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates/import/file")
@Tag(name = "Импорт из файла")
public class NotFoundToExcelController {

    private final NotFoundToExcelService service;

    @PostMapping("/not-found-to-excel")
    @Operation(
            summary = "Экспорт result-not-found.json в Excel",
            description = """
                    Читает JSON, классифицирует номера на «возможно валидные» и «мусор»,
                    пишет XLSX с листами:
                    - Возможно валидные
                    - Мусор
                    - Все подряд
                    """
    )
    public ResponseEntity<NotFoundExportResult> export(
            @Valid @RequestBody NotFoundToExcelRequest request) throws IOException {
        return ResponseEntity.ok(service.export(request));
    }
}