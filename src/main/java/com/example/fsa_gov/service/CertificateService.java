package com.example.fsa_gov.service;

import com.example.fsa_gov.client.FsaClient;
import com.example.fsa_gov.dto.*;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Сервисная логика для работы с сертификатами соответствия РФ.
 */
@Service
public class CertificateService {

    private final FsaClient fsaClient;

    public CertificateService(FsaClient fsaClient) {
        this.fsaClient = fsaClient;
    }

    /**
     * Получает информацию о сертификате по номеру документа.
     * 
     * @param request DTO с данными запроса (numberDoc, regDate, applicantInn)
     * @return Mono<CertificateResponseDto> — ответ API ФСА или ошибку
     */
    public Mono<CertificateResponseDto> getCertificate(CertificateRequestDto request) {
        return fsaClient.getCertificate(request);
    }

}
