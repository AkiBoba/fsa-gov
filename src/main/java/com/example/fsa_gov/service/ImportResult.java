package com.example.fsa_gov.service;

import lombok.Data;

/**
 * Результат запуска асинхронного импорта.
 */
@Data
public class ImportResult {
    /** requestId для проверки СС РФ (null, если не запускалось или ошибка) */
    private String rfRequestId;
    /** requestId для проверки ЕАЭС (null, если не запускалось или ошибка) */
    private String eaeuRequestId;
    /** Ошибка запуска для РФ (если была) */
    private String rfError;
    /** Ошибка запуска для ЕАЭС (если была) */
    private String eaeuError;

    public boolean isRfStarted()   { return rfRequestId != null; }
    public boolean isEaeuStarted() { return eaeuRequestId != null; }
}