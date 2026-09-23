package com.example.fsa_gov.service.job;

import lombok.Data;
import java.time.Instant;
import java.util.List;

/**
 * Состояние задачи импорта.
 * Хранится в памяти (ConcurrentHashMap) до истечения TTL.
 */
@Data
public class ImportJobState {

    private String jobId;
    private JobStatus status;          // PENDING, RUNNING, SUCCESS, FAILED
    private String errorMessage;       // если FAILED

    private Instant createdAt;
    private Instant updatedAt;

    // Промежуточные requestId
    private String rfRequestId;
    private String eaeuRequestId;
    private String fallbackRfRequestId;
    private String fallbackEaeuRequestId;

    // Итоговые списки
    private List<String> rfFound;
    private List<String> eaeuFound;
    private List<String> rfNotFound;
    private List<String> eaeuNotFound;
    private List<String> invalidEntries;

    public enum JobStatus { PENDING, RUNNING, SUCCESS, FAILED }
}