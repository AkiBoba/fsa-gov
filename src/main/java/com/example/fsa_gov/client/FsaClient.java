package com.example.fsa_gov.client;

import com.example.fsa_gov.dto.CertificateRequestDto;
import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.dto.async.AsyncFindDocRequest;
import com.example.fsa_gov.dto.async.AsyncFindDocResponse;
import com.example.fsa_gov.exception.FsaApiException;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Клиент для взаимодействия с API ФСА (ФГИС Росаккредитация).
 */
@Component
public class FsaClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FsaClient(@Qualifier("fsaWebClient") WebClient fsaWebClient,
                     ObjectMapper objectMapper) {
        this.webClient = fsaWebClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Получает информацию по одному сертификату соответствия РФ (СС).
     *
     * @param request DTO с данными запроса (numberDoc, regDate, applicantInn)
     * @return Mono&lt;CertificateResponseDto&gt; — ответ API ФСА или ошибку
     */
    public Mono<CertificateResponseDto> getCertificate(CertificateRequestDto request) {
        return webClient.post()
                .uri("/sync/rss/get")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                // Обработка ошибок: любой 4xx/5xx → FsaApiException с телом ответа
                .onStatus(
                        status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> parseError(response.statusCode().value(), body))
                )
                .bodyToMono(CertificateResponseDto.class);
    }

    /**
     * Пытается распарсить тело ошибки ФСА в структурированный FsaApiException.
     * Если не получается — возвращает исключение только с кодом.
     */
    private FsaApiException parseError(int statusCode, String body) {
        if (body == null || body.isBlank()) {
            return new FsaApiException(statusCode);
        }
        try {
            // Предполагаем, что тело ошибки соответствует FsaApiErrorResponse
            var errorResponse = objectMapper.readValue(
                    body,
                    com.example.fsa_gov.response.FsaApiErrorResponse.class
            );
            return new FsaApiException(
                    statusCode,
                    errorResponse.getCode(),
                    errorResponse.getDetail() != null
                            ? errorResponse.getDetail()
                            : errorResponse.getDescription(),
                    errorResponse.getInstance()
            );
        } catch (Exception e) {
            // Если не удалось распарсить — возвращаем просто код
            return new FsaApiException(statusCode);
        }
    }

    /**
     * Асинхронный поиск по списку СС РФ.
     */
    public AsyncFindDocResponse asyncRssFindDoc(AsyncFindDocRequest request) {
        return webClient.post()
                .uri("/async/rss/find-doc")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> parseError(response.statusCode().value(), body)))
                .bodyToMono(AsyncFindDocResponse.class)
                .block(); // для Spring MVC — блокируем
    }

    /**
     * Асинхронный поиск по списку документов ЕАЭС.
     */
    public AsyncFindDocResponse asyncReaeuFindDoc(AsyncFindDocRequest request) {
        return webClient.post()
                .uri("/async/reaeu/find-doc")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(body -> parseError(response.statusCode().value(), body)))
                .bodyToMono(AsyncFindDocResponse.class)
                .block();
    }
}