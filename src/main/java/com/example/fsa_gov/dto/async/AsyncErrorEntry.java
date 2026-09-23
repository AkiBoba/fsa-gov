package com.example.fsa_gov.dto.async;

import lombok.Data;

@Data
public class AsyncErrorEntry {
    private String numberDoc;
    private String regDate;       // nullable
    private String applicantInn;  // nullable
    private String reason;        // "not_found", "multiple_matches", "lookup_error"
    private Integer matchCount;   // nullable
}