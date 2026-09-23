package com.example.fsa_gov.controller;

import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.parser.dto.ParseResult;
import com.example.fsa_gov.service.CertificateImportService;
import com.example.fsa_gov.service.ImportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates/import")
@Tag(name = "Импорт сертификатов", description = "Массовая проверка списка сертификатов")
public class CertificateImportController {

    private final CertificateImportService importService;

    @PostMapping("/parse")
    @Operation(summary = "Распарсить список сертификатов без проверки")
    public ResponseEntity<ParseResult> parse(@RequestBody List<String> lines) {
        return ResponseEntity.ok(importService.parseLines(lines));
    }

    @PostMapping("/start")
    @Operation(summary = "Распарсить и запустить асинхронную проверку")
    public ResponseEntity<ImportResult> start(@RequestBody List<String> lines) {
        ParseResult parsed = importService.parseLines(lines);
        return ResponseEntity.ok(importService.startImport(parsed));
    }

    @PostMapping("/start-with-fallback")
    @Operation(summary = "Запустить импорт с fallback для not_found")
    public ResponseEntity<ImportResult> startWithFallback(@RequestBody List<String> lines) {
        ParseResult parsed = importService.parseLines(lines);
        return ResponseEntity.ok(importService.startImportWithFallback(parsed));
    }

    @GetMapping("/result/{requestId}")
    @Operation(summary = "Скачать результат по requestId")
    public ResponseEntity<List<CertificateResponseDto>> getResult(
            @PathVariable String requestId) {
        return ResponseEntity.ok(
                importService.readResult(requestId, CertificateResponseDto.class));
    }
}