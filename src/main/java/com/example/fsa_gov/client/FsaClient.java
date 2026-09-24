package com.example.fsa_gov.client;

import com.example.fsa_gov.dto.CertificateRequestDto;
import com.example.fsa_gov.dto.CertificateResponseDto;
import com.example.fsa_gov.dto.async.AsyncFindDocRequest;
import com.example.fsa_gov.dto.async.AsyncFindDocResponse;
import com.example.fsa_gov.dto.async.AsyncStatusResponse;
import com.example.fsa_gov.exception.FsaApiException;
import com.example.fsa_gov.response.FsaApiErrorResponse;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Клиент для взаимодействия с API ФСА (ФГИС Росаккредитация).
 * Все ошибки приводятся к {@link FsaApiException}.
 */
@Slf4j
@Component
public class FsaClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public FsaClient(@Qualifier("fsaWebClient") WebClient fsaWebClient,
                     ObjectMapper objectMapper) {
        this.webClient = fsaWebClient;
        this.objectMapper = objectMapper;
    }

    /* ======================= SYNC ======================= */

    public CertificateResponseDto getCertificate(CertificateRequestDto request) {
        return webClient.post()
                .uri("/sync/rss/get")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(CertificateResponseDto.class)
                .block();
    }

    /* ======================= ASYNC ======================= */

    public AsyncFindDocResponse asyncRssFindDoc(AsyncFindDocRequest request) {
        return webClient.post()
                .uri("/async/rss/find-doc")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(AsyncFindDocResponse.class)
                .block();
    }

    public AsyncFindDocResponse asyncReaeuFindDoc(AsyncFindDocRequest request) {
        return webClient.post()
                .uri("/async/reaeu/find-doc")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(AsyncFindDocResponse.class)
                .block();
    }

    public AsyncStatusResponse asyncRequestStatus(String requestId) {
        return webClient.get()
                .uri("/async/status/{requestId}", requestId)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(AsyncStatusResponse.class)
                .block();
    }

    public byte[] asyncRequestResult(String requestId) {
        return webClient.get()
                .uri("/async/result/{requestId}", requestId)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(byte[].class)
                .block();
    }

    public byte[] asyncRequestErrors(String requestId) {
        return webClient.get()
                .uri("/async/errors/{requestId}", requestId)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleError)
                .bodyToMono(byte[].class)
                .block();
    }

    /* ======================= ERROR ======================= */

    private Mono<? extends Throwable> handleError(ClientResponse response) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> parseError(response.statusCode().value(), body));
    }

    private FsaApiException parseError(int statusCode, String body) {
        if (body == null || body.isBlank()) {
            return new FsaApiException(statusCode);
        }
        try {
            FsaApiErrorResponse err = objectMapper.readValue(body, FsaApiErrorResponse.class);
            String detail = err.getDetail() != null ? err.getDetail() : err.getDescription();
            return new FsaApiException(statusCode, err.getCode(), detail, err.getInstance());
        } catch (Exception e) {
            log.debug("Не удалось распарсить тело ошибки ФСА: {}", body, e);
            return new FsaApiException(statusCode);
        }
    }
}