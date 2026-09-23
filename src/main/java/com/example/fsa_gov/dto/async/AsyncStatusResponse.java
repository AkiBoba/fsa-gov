package com.example.fsa_gov.dto.async;

import lombok.Data;

@Data
public class AsyncStatusResponse {
    private String requestId;
    private String section;      // "RSS", "RDS", "REAEU"
    private String kind;         // "FIND_DOC", "CHECK_STATUS"
    private String status;       // PENDING, RUNNING, SUCCESS, FAILED
    private String createdAt;
    private String updatedAt;
    private String errorMessage; // nullable
    private Boolean resultAvailable;
    private Boolean errorsAvailable;
}