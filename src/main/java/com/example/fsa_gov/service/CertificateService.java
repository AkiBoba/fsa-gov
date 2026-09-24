package com.example.fsa_gov.service;

import com.example.fsa_gov.client.FsaClient;
import com.example.fsa_gov.dto.CertificateRequestDto;
import com.example.fsa_gov.dto.CertificateResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final FsaClient fsaClient;

    public CertificateResponseDto getCertificate(CertificateRequestDto request) {
        return fsaClient.getCertificate(request);
    }

    public CertificateResponseDto getEaeuCertificate(CertificateRequestDto request) {
        return fsaClient.getEaeuCertificate(request);
    }
}