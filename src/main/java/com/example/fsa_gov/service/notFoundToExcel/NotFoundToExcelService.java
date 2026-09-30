package com.example.fsa_gov.service.notFoundToExcel;

import com.example.fsa_gov.dto.notFoundToExcel.NotFoundExportResult;
import com.example.fsa_gov.dto.notFoundToExcel.NotFoundFileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotFoundToExcelService {

    private final ObjectMapper objectMapper;

    /* ===================== JUNK ===================== */

    /** Явные текстовые маркеры мусора — в начале строки. */
    private static final Pattern JUNK_PREFIX = Pattern.compile(
            "^(Письмо|ПИСЬМО|Пиьсмо|Пиьсо|Пиcьмо|Птсьмо|" +
                    "Решение|РЕШЕНИЕ|Отказное|ОТКАЗНОЕ|" +
                    "Паспорт|ПАСПОРТ|Уведомление|Удостоверение|Свидетельство|Справка|" +
                    "Сертификат б/н|Сертификат качества|Сертификат на|Сертификат №|" +
                    "тест|ТЕСТ|Тест|" +
                    "СДС|VCS-IST|ПАО ПСМ|" +
                    "\\*Товар без сертификата|" +
                    "Информационное письмо|Отказное решение|Удостоверение о качестве|" +
                    "Приложение|ПРИЛОЖЕНИЕ|Прил\\.|" +
                    "см\\.|См\\.|СМ\\.)" +
                    ".*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Явные «мусорные» подстроки где угодно в строке. */
    private static final Pattern JUNK_ANYWHERE = Pattern.compile(
            ".*(б/н|Б/Н|без номера|без №|" +
                    "сертификат\\s+не\\s+требуется|" +
                    "не\\s+подлежит|" +
                    "отсутствует|" +
                    "нет\\s+данных|" +
                    "аннулирован|" +
                    "архив|" +
                    "дубликат|" +
                    "копия|" +
                    "фото|" +
                    "скан).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** РФ-префикс — чтобы отличить РФ от ЕАЭС. */
    private static final Pattern RF_PREFIX = Pattern.compile(
            "^(РОСС\\s+RU|РОСС\\s+[A-Z]{2}\\.|РООС\\s+RU|ТС\\s+RU|RU\\.\\d|\\d{6}\\/).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Хотя бы одна цифра в строке — минимальный признак «номера». */
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

    private record Entry(String number, String source) {}

    /* ===================== PUBLIC ===================== */

    public NotFoundExportResult export(MultipartFile file,
                                       String excelFilePath,
                                       boolean splitSheets) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран или пуст");
        }

        // 1. Читаем JSON (совместимо со старым и новым форматом)
        NotFoundFileDto json;
        try {
            json = objectMapper.readValue(file.getInputStream(), NotFoundFileDto.class);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Не удалось прочитать JSON из файла " + file.getOriginalFilename()
                            + ". Убедитесь, что это result-not-found.json, а не .xlsx/.txt. "
                            + "Причина: " + e.getMessage(), e);
        }

        List<String> allNotFound = json.getAllNotFound();

        // 2. Классификация
        List<Entry> rfValid   = new ArrayList<>();
        List<Entry> eaeuValid = new ArrayList<>();
        List<Entry> rfJunk    = new ArrayList<>();
        List<Entry> eaeuJunk  = new ArrayList<>();

        for (String raw : allNotFound) {
            if (raw == null) continue;

            String number = normalize(raw);
            if (number.isEmpty()) {
                // пустую строку тоже относим к мусору (в ЕАЭС-ведро)
                eaeuJunk.add(new Entry(raw, "ЕАЭС"));
                continue;
            }

            boolean isRf   = RF_PREFIX.matcher(number).matches();
            boolean isJunk = isJunk(number);

            if (isRf) {
                if (isJunk) rfJunk.add(new Entry(number, "RF"));
                else        rfValid.add(new Entry(number, "RF"));
            } else {
                if (isJunk) eaeuJunk.add(new Entry(number, "ЕАЭС"));
                else        eaeuValid.add(new Entry(number, "ЕАЭС"));
            }
        }

        log.info("not-found-to-excel: вход={}, RF valid={}, RF junk={}, ЕАЭС valid={}, ЕАЭС junk={}",
                allNotFound.size(), rfValid.size(), rfJunk.size(), eaeuValid.size(), eaeuJunk.size());

        // 3. Пишем XLSX
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            if (splitSheets) {
                writeSheet(book, "Возможно валидные", concat(rfValid, eaeuValid));
                writeSheet(book, "Мусор",            concat(rfJunk, eaeuJunk));
                writeSheet(book, "Все подряд",       concat(concat(rfValid, eaeuValid),
                        concat(rfJunk, eaeuJunk)));
            } else {
                writeSheet(book, "Все", concat(concat(rfValid, eaeuValid),
                        concat(rfJunk, eaeuJunk)));
            }

            Path out = Path.of(excelFilePath);
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
                book.write(fos);
            }
        }

        NotFoundExportResult result = new NotFoundExportResult();
        result.setRfTotal(rfValid.size() + rfJunk.size());
        result.setEaeuTotal(eaeuValid.size() + eaeuJunk.size());
        result.setRfValid(rfValid.size());
        result.setEaeuValid(eaeuValid.size());
        result.setRfJunk(rfJunk.size());
        result.setEaeuJunk(eaeuJunk.size());
        result.setExcelPath(excelFilePath);
        return result;
    }

    /* ===================== JUNK LOGIC ===================== */

    /**
     * Мусор — это:
     *  - пустая строка;
     *  - строка без единой цифры;
     *  - строка, начинающаяся с мусорного маркера (Письмо, Решение, …);
     *  - строка, содержащая мусорную подстроку (б/н, без номера, архив, …);
     *  - слишком короткая строка (< 5 символов), если в ней нет цифр.
     */
    private boolean isJunk(String s) {
        if (s == null || s.isBlank()) return true;

        if (!HAS_DIGIT.matcher(s).matches()) return true;

        if (JUNK_PREFIX.matcher(s).matches()) return true;
        if (JUNK_ANYWHERE.matcher(s).matches()) return true;

        // Очень короткие «номера» типа "1", "12", "abc" — мусор
        return s.length() < 5;
    }

    /** Убирает BOM/неразрывные пробелы, схлопывает пробелы. */
    private String normalize(String s) {
        return s.replace("\uFEFF", "")
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    /* ===================== HELPERS ===================== */

    private List<Entry> concat(List<Entry> a, List<Entry> b) {
        List<Entry> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    private void writeSheet(XSSFWorkbook book, String name, List<Entry> entries) {
        Sheet sheet = book.createSheet(name);

        CellStyle headerStyle = createHeaderStyle(book);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("№");
        header.createCell(1).setCellValue("Номер сертификата");
        header.createCell(2).setCellValue("Источник");
        for (int i = 0; i <= 2; i++) header.getCell(i).setCellStyle(headerStyle);

        int rowIdx = 1;
        for (Entry e : entries) {
            Row row = sheet.createRow(rowIdx);
            row.createCell(0).setCellValue(rowIdx);
            row.createCell(1).setCellValue(e.number());
            row.createCell(2).setCellValue(e.source());
            rowIdx++;
        }

        sheet.setColumnWidth(0, 1500);
        sheet.setColumnWidth(1, 12000);
        sheet.setColumnWidth(2, 3000);
        sheet.createFreezePane(0, 1);
    }

    private CellStyle createHeaderStyle(XSSFWorkbook book) {
        CellStyle style = book.createCellStyle();
        Font font = book.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}