package com.example.fsa_gov.parser.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * Результат парсинга списка сертификатов.
 */
@Data
@Builder
public class ParseResult {
    /** Сертификаты РФ (Росаккредитация) — для asyncRssFindDoc */
    private List<String> rfCertificates;
    /** Сертификаты ЕАЭС — для asyncReaeuFindDoc */
    private List<String> eaeuCertificates;
    /** Нераспознанные / некорректные строки */
    private List<String> invalidEntries;
}
