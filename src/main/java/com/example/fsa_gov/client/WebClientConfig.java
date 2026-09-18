package com.example.fsa_gov.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Конфигурация WebClient для работы с API ФСА.
 */
@Configuration
public class WebClientConfig {

    @Value("${fsa.api.base-url}")
    private String baseUrl;

    @Value("${fsa.api.key:}")
    private String apiKey;

    /**
     * Создает настроенный WebClient с базовым URL и заголовками авторизации.
     *
     * Имя бина — fsaWebClient, чтобы не конфликтовать с классом FsaClient,
     * который Spring автоматически регистрирует как бин "fsaClient".
     */
    @Bean
    public WebClient fsaWebClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .defaultHeader("API-Key", apiKey != null && !apiKey.isBlank() ? apiKey : "")
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1048576))
                .build();
    }
}