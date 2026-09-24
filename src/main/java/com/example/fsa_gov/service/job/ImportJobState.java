package com.example.fsa_gov.service.job;

import com.example.fsa_gov.dto.CertificateResponseDto;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ImportJobState {

    private String jobId;
    private JobStatus status;
    private String errorMessage;

    private Instant createdAt;
    private Instant updatedAt;

    private String rfRequestId;
    private String eaeuRequestId;
    private String fallbackRfRequestId;
    private String fallbackEaeuRequestId;

    /** Полные найденные документы (РФ) */
    private List<CertificateResponseDto> rfFound;
    /** Полные найденные документы (ЕАЭС) */
    private List<CertificateResponseDto> eaeuFound;

    /** Номера, которые ФСА не нашёл (оставляем строками) */
    private List<String> rfNotFound;
    private List<String> eaeuNotFound;
    private List<String> invalidEntries;

    public enum JobStatus { PENDING, RUNNING, SUCCESS, FAILED }
}