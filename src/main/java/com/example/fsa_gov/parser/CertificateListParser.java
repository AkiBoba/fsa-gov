package com.example.fsa_gov.parser;

import com.example.fsa_gov.parser.dto.ParseResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class CertificateListParser {

    /** Убирает "Сертификаты номенклатуры" с учётом неразрывных пробелов */
    private static final Pattern NOMENCLATURE_PREFIX =
            Pattern.compile("Сертификаты[\\s\\p{Zs}]+номенклатуры[\\s\\p{Zs}]*",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Маркеры ЕАЭС — проверяются по нормализованной строке */
    private static final Pattern EAES_PREFIX =
            Pattern.compile("^(ЕАЭС|ТС\\s*[-\\s]?[BВ]?[YУ]?)[\\s\\p{Zs}].*",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Маркеры РФ — проверяются по нормализованной строке */
    private static final Pattern RF_PREFIX =
            Pattern.compile("^(РОСС\\s+RU|ТС\\s+RU|RU\\.\\d|\\d{6}\\/).*",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Явный мусор */
    private static final Pattern JUNK_PATTERN =
            Pattern.compile("^(Письмо|Решение|тест|Белкард|Письмо_|Письмо\\s+№).*",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Похожие символы: латиница → кириллица */
    private static final String LATIN  = "EABCOPKMTHXYeabcopkmthxy";
    private static final String CYRIL  = "ЕАВСОРКМТНХУеаьсоркмтнху";

    public ParseResult parse(List<String> lines) {
        List<String> rf = new ArrayList<>();
        List<String> eaeu = new ArrayList<>();
        List<String> invalid = new ArrayList<>();

        if (lines == null || lines.isEmpty()) {
            return ParseResult.builder()
                    .rfCertificates(rf)
                    .eaeuCertificates(eaeu)
                    .invalidEntries(invalid)
                    .build();
        }

        for (String rawLine : lines) {
            if (rawLine == null) {
                invalid.add(null);
                continue;
            }

            // 1. Убираем BOM и неразрывные пробелы
            String normalized = rawLine
                    .replace("\uFEFF", "")
                    .replace('\u00A0', ' ')
                    .trim();

            // 2. Убираем префикс "Сертификаты номенклатуры"
            String cleaned = NOMENCLATURE_PREFIX.matcher(normalized).replaceAll("").trim();

            // 3. Пустая строка
            if (cleaned.isEmpty()) {
                invalid.add(rawLine);
                continue;
            }

            // 4. Явный мусор
            if (JUNK_PATTERN.matcher(cleaned).matches()) {
                invalid.add(rawLine);
                continue;
            }

            // 5. Нормализуем "похожие" символы ТОЛЬКО для проверки
            String check = normalizeSimilarChars(cleaned);

            // 6. Классифицируем по нормализованной строке
            if (EAES_PREFIX.matcher(check).matches()) {
                eaeu.add(cleaned);            // сохраняем оригинал
            } else if (RF_PREFIX.matcher(check).matches()) {
                rf.add(cleaned);              // сохраняем оригинал
            } else {
                invalid.add(rawLine);
            }
        }

        return ParseResult.builder()
                .rfCertificates(rf)
                .eaeuCertificates(eaeu)
                .invalidEntries(invalid)
                .build();
    }

    /**
     * Заменяет латинские символы, визуально похожие на кириллические,
     * на кириллические — только в первых 25 символах.
     */
    private String normalizeSimilarChars(String input) {
        if (input == null) return null;
        int limit = Math.min(25, input.length());
        StringBuilder sb = new StringBuilder(input);
        for (int i = 0; i < limit; i++) {
            char c = sb.charAt(i);
            int idx = LATIN.indexOf(c);
            if (idx >= 0) {
                sb.setCharAt(i, CYRIL.charAt(idx));
            }
        }
        return sb.toString();
    }
}