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

/**
 * REST-контроллер для работы с сертификатами соответствия РФ.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/certificates")
@Tag(name = "Сертификаты (СС) РФ",
        description = "Работа с реестром сертификатов соответствия Российской Федерации")
public class CertificateController {

    private final CertificateService certificateService;

    /**
     * Получение информации по одному сертификату соответствия РФ.
     */
    @PostMapping("/rss/get")
    @Operation(
            summary = "Получение сертификата соответствия РФ",
            description = """
            Получает полную информацию по одному сертификату соответствия (СС) реестра РФ.

            **Важно:**
            - Указывайте номер документа в формате ФГИС Росаккредитации
              (например: "РОСС RU С-RU.HB34.B.00690/26")
            - Если по номеру найдено несколько документов — укажите regDate
              или applicantInn для уточнения

            **Возможные статусы ответа:**
            - 200 OK: документ найден и возвращён с полной информацией
            - 401 Unauthorized: неверный API-Key
            - 403 Forbidden: нет прав доступа к методу
            - 404 Not Found: документ не найден в реестре СС РФ
            - 409 Conflict: найдено несколько совпадений — укажите regDate или applicantInn

            **Лимит запросов:** 1000/час на организацию
            """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Параметры поиска сертификата.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("""
                    {
                      "numberDoc": "РОСС RU С-RU.HB34.B.00690/26",
                      "regDate": "2026-07-17",
                      "applicantInn": "7706114267"
                    }
                    """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Успешное получение данных о сертификате",
                            content = @Content(mediaType = "application/json")),
                    @ApiResponse(responseCode = "401", description = "Неверный API-Key"),
                    @ApiResponse(responseCode = "403", description = "Нет прав доступа к методу"),
                    @ApiResponse(responseCode = "404", description = "Документ не найден"),
                    @ApiResponse(responseCode = "409",
                            description = "Найдено несколько совпадений — укажите regDate или applicantInn"),
                    @ApiResponse(responseCode = "500", description = "Ошибка сервера API ФСА")
            }
    )
    public ResponseEntity<CertificateResponseDto> getCertificate(
            @Valid @RequestBody CertificateRequestDto request) {

        // Блокирующее получение результата (Spring MVC, не WebFlux).
        // Ошибки (FsaApiException) обрабатываются GlobalExceptionHandler.
        CertificateResponseDto response = certificateService.getCertificate(request).block();

        return ResponseEntity.ok(response);
    }
}