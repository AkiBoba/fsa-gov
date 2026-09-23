package com.example.fsa_gov.service;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Результат запуска асинхронного импорта.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    /** requestId для проверки СС РФ */
    private String rfRequestId;
    /** requestId для проверки ЕАЭС */
    private String eaeuRequestId;
    /** requestId для fallback-проверки РФ-документов в ЕАЭС */
    private String fallbackEaeuRequestId;
    /** requestId для fallback-проверки ЕАЭС-документов в РФ */
    private String fallbackRfRequestId;
    /** Ошибка запуска для РФ */
    private String rfError;
    /** Ошибка запуска для ЕАЭС */
    private String eaeuError;

    public boolean isRfStarted()          { return rfRequestId != null; }
    public boolean isEaeuStarted()        { return eaeuRequestId != null; }
    public boolean isFallbackRfStarted()  { return fallbackRfRequestId != null; }
    public boolean isFallbackEaeuStarted(){ return fallbackEaeuRequestId != null; }
}