package com.example.fsa_gov.dto.async;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Один документ в запросе asyncFindDoc.
 * Для РФ: numberDoc + regDate + applicantInn.
 * Для ЕАЭС: numberDoc + regDate.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FindDocItem {
    private String numberDoc;
    private String regDate;       // nullable
    private String applicantInn;  // nullable, только для РФ
}