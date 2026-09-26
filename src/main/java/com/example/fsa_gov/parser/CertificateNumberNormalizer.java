package com.example.fsa_gov.parser;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Нормализует номер сертификата/декларации к «кириллическому» виду,
 * сохраняя коды стран (RU, DE, CN, ...) в латинице.
 *
 * Используется в диагностическом endpoint /normalize-check.
 *
 * Что делает:
 *  1) trim + сворачивает пробелы;
 *  2) убирает хвостовые мусорные символы (/// , ,, , .., .);
 *  3) убирает пробел после точки;
 *  4) заменяет одиночные запятые/слэши внутри номера на точки;
 *  5) чинит опечатки в префиксе (TC → ТС, POCC → РОСС и т.п.);
 *  6) заменяет латинские буквы, похожие на кириллические, на кириллические,
 *     КРОМЕ двухбуквенных кодов стран.
 *
 * ВАЖНО: не трогает коды стран (RU, DE, CN, CZ, JP, US, TR, BG, BY, UA,
 * IT, FR, ES, KR, RS, HK, GB, PL, SE, FI, BE, NL, AT, CH, IN, TW, HU,
 * SK, LT, LV, EE, SI, HR, GR, PT, RO, NO, DK, IE, CA, BR, MX, VN, TH,
 * ID, MY, SG, PH) — они всегда остаются латиницей.
 */
@Component
public class CertificateNumberNormalizer {

    /** Коды стран — оставляем латиницей. Без дубликатов! */
    private static final Set<String> COUNTRY_CODES = Set.of(
            "RU", "DE", "CN", "CZ", "JP", "US", "TR", "BG", "BY", "UA",
            "IT", "FR", "ES", "KR", "RS", "HK", "GB", "PL", "SE", "FI",
            "BE", "NL", "AT", "CH", "IN", "TW", "HU", "SK", "LT", "LV",
            "EE", "SI", "HR", "GR", "PT", "RO", "NO", "DK", "IE", "CA",
            "BR", "MX", "VN", "TH", "ID", "MY", "SG", "PH"
    );

    /* ======================= PUBLIC API ======================= */

    /**
     * Нормализует один номер. Возвращает либо нормализованную строку,
     * либо исходную, если вход null/blank.
     */
    public String normalize(String number) {
        if (number == null || number.isBlank()) {
            return number;
        }

        // 1. Trim + свернуть пробелы
        String s = number.trim().replaceAll("\\s+", " ");

        // 2. Убрать хвостовые мусорные символы (2+ подряд, потом одиночные)
        s = s.replaceAll("[/,.]{2,}$", "");
        s = s.replaceAll("[/,.]$", "");

        // 3. Убрать пробел после точки
        s = s.replaceAll("\\.\\s+", ".");

        // 4. Заменить одиночные запятые/слэши внутри номера на точки
        s = s.replaceAll("(?<=[А-ЯA-Z0-9]),\\s*(?=[0-9])", ".");
        s = s.replaceAll("(?<=[А-ЯA-Z0-9])/\\s*(?=[0-9])", ".");

        // 5. Специальные замены префиксов и опечаток
        s = s.replaceFirst("^TC\\b", "ТС");
        s = s.replaceFirst("^POCC\\b", "РОСС");
        s = s.replaceFirst("^Росс\\b", "РОСС");
        s = s.replaceFirst("^РООС\\b", "РОСС");

        // 6. Заменить латиницу на кириллицу, кроме кодов стран
        s = normalizeAlphabet(s);

        return s;
    }

    /* ======================= ALPHABET ======================= */

    /**
     * Проходит по строке и заменяет латинские символы, похожие на
     * кириллические, на кириллические — КРОМЕ кодов стран.
     */
    private String normalizeAlphabet(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            // Проверяем: не начинается ли здесь код страны?
            if (isCountryCodeStart(s, i)) {
                String code = s.substring(i, i + 2).toUpperCase();
                sb.append(code);
                i += 2;
                continue;
            }

            // Иначе — заменяем латиницу на кириллицу
            char c = s.charAt(i);
            sb.append(latinToCyrillic(c));
            i++;
        }
        return sb.toString();
    }

    /**
     * Проверяет, начинается ли в позиции i код страны из COUNTRY_CODES.
     *
     * Условия:
     *  - перед кодом не буква и не цифра (начало строки, пробел, дефис, точка);
     *  - после кода — точка, дефис, пробел или конец строки.
     */
    private boolean isCountryCodeStart(String s, int i) {
        if (i + 2 > s.length()) return false;

        // Перед кодом не должно быть буквы/цифры
        if (i > 0 && Character.isLetterOrDigit(s.charAt(i - 1))) return false;

        String candidate = s.substring(i, i + 2).toUpperCase();
        if (!COUNTRY_CODES.contains(candidate)) return false;

        // После кода — конец строки или разделитель
        if (i + 2 == s.length()) return true;
        char next = s.charAt(i + 2);
        return next == '.' || next == '-' || next == ' ';
    }

    /* ======================= CHAR TABLE ======================= */

    /**
     * Заменяет латинский символ на визуально похожий кириллический.
     * Если соответствия нет — возвращает символ как есть.
     */
    private char latinToCyrillic(char c) {
        return switch (c) {
            case 'A' -> 'А';
            case 'B' -> 'В';
            case 'C' -> 'С';
            case 'E' -> 'Е';
            case 'H' -> 'Н';
            case 'K' -> 'К';
            case 'M' -> 'М';
            case 'O' -> 'О';
            case 'P' -> 'Р';
            case 'T' -> 'Т';
            case 'X' -> 'Х';
            case 'Y' -> 'У';
            case 'a' -> 'а';
            case 'b' -> 'ь';
            case 'c' -> 'с';
            case 'e' -> 'е';
            case 'h' -> 'н';
            case 'k' -> 'к';
            case 'm' -> 'м';
            case 'o' -> 'о';
            case 'p' -> 'р';
            case 't' -> 'т';
            case 'x' -> 'х';
            case 'y' -> 'у';
            default -> c;
        };
    }
}