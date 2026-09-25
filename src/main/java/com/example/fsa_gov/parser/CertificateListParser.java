package com.example.fsa_gov.parser;

import com.example.fsa_gov.parser.dto.ParseResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Парсер списка сертификатов.
 *
 * Логика:
 *  - убираем только технический префикс "Сертификаты номенклатуры";
 *  - если строка явно похожа на РФ-формат — отправляем в РФ;
 *  - всё остальное — в ЕАЭС;
 *  - решение "мусор/не мусор" принимает API ФСА (вернёт not_found).
 */
@Component
public class CertificateListParser {

    /** Убирает "Сертификаты номенклатуры" с учётом неразрывных пробелов */
    private static final Pattern NOMENCLATURE_PREFIX =
            Pattern.compile("Сертификаты[\\s\\p{Zs}]+номенклатуры[\\s\\p{Zs}]*",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * РФ-маркеры (проверяются по нормализованной строке):
     *   РОСС RU..., РОСС XX...., ТС RU..., RU.xx..., NNNNNN/...
     */
    private static final Pattern RF_PREFIX =
            Pattern.compile("^(РОСС\\s+RU|РОСС\\s+[A-Z]{2}\\.|РООС\\s+RU|ТС\\s+RU|RU\\.\\d|\\d{6}\\/).*",
                    Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Похожие символы: латиница → кириллица (только для проверки) */
    private static final String LATIN = "EABCOPKMTHXYeabcopkmthxy";
    private static final String CYRIL = "ЕАВСОРКМТНХУеаьсоркмтнху";

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

            // 3. Пустая строка — только это считаем "invalid"
            if (cleaned.isEmpty()) {
                invalid.add(rawLine);
                continue;
            }

            // 4. Нормализуем "похожие" символы для проверки
            String check = normalizeSimilarChars(cleaned);

            // 5. РФ-формат — в РФ, всё остальное — в ЕАЭС
            if (RF_PREFIX.matcher(check).matches()) {
                rf.add(cleaned);
            } else {
                eaeu.add(cleaned);
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