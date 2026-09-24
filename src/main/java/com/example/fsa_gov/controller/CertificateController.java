package com.example.fsa_gov.controller;

import com.example.fsa_gov.dto.CertificateRequestDto;
import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates")
@Tag(name = "Сертификаты (СС) РФ",
        description = "Работа с реестром сертификатов соответствия Российской Федерации")
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/rss/get")
    @Operation(
            summary = "Получение сертификата соответствия РФ",
            description = "Возвращает полную информацию по одному сертификату СС РФ.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject("""
                    {
                      "numberDoc": "РОСС RU С-RU.HB34.B.00690/26",
                      "regDate": "2026-07-17",
                      "applicantInn": "7706114267"
                    }
                    """))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK"),
                    @ApiResponse(responseCode = "401", description = "Неверный API-Key"),
                    @ApiResponse(responseCode = "403", description = "Нет прав доступа"),
                    @ApiResponse(responseCode = "404", description = "Документ не найден"),
                    @ApiResponse(responseCode = "409", description = "Несколько совпадений"),
                    @ApiResponse(responseCode = "500", description = "Ошибка API ФСА")
            }
    )
    public ResponseEntity<CertificateResponseDto> getCertificate(
            @Valid @RequestBody CertificateRequestDto request) {
        return ResponseEntity.ok(certificateService.getCertificate(request));
    }

    @PostMapping("/reaeu/get")
    @Operation(summary = "Получение сертификата ЕАЭС")
    public ResponseEntity<CertificateResponseDto> getEaeuCertificate(
            @Valid @RequestBody CertificateRequestDto request) {
        return ResponseEntity.ok(certificateService.getEaeuCertificate(request));
    }
}