package com.example.fsa_gov.service.notFoundToExcel;

import com.example.fsa_gov.dto.notFoundToExcel.NotFoundExportResult;
import com.example.fsa_gov.dto.notFoundToExcel.NotFoundFileDto;
import com.example.fsa_gov.dto.notFoundToExcel.NotFoundToExcelRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
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

    private static final Pattern JUNK_PATTERN = Pattern.compile(
            "^(Письмо|ПИСЬМО|Пиьсмо|Пиьсо|Решение|РЕШЕНИЕ|Отказное|ОТКАЗНОЕ|" +
                    "Паспорт|ПАСПОРТ|Уведомление|Удостоверение|Свидетельство|Справка|" +
                    "Сертификат б/н|Сертификат качества|Сертификат на|Сертификат №|" +
                    "тест|СДС|VCS-IST|ПАО ПСМ|\\*Товар без сертификата|" +
                    "Информационное письмо|Отказное решение|Удостоверение о качестве).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Обёртка одной строки: номер + источник (RF / ЕАЭС). */
    private record Entry(String number, String source) {}

    public NotFoundExportResult export(NotFoundToExcelRequest req) throws IOException {
        // 1. Читаем JSON
        NotFoundFileDto json = objectMapper.readValue(
                Path.of(req.getJsonFilePath()).toFile(), NotFoundFileDto.class);

        List<String> rf   = json.getRfNotFound()   != null ? json.getRfNotFound()   : List.of();
        List<String> eaeu = json.getEaeuNotFound() != null ? json.getEaeuNotFound() : List.of();

        // 2. Разделяем на valid / junk, оборачивая в Entry с источником
        List<Entry> rfValidEntries   = wrap(filterValid(rf),   "RF");
        List<Entry> rfJunkEntries    = wrap(filterJunk(rf),    "RF");
        List<Entry> eaeuValidEntries = wrap(filterValid(eaeu), "ЕАЭС");
        List<Entry> eaeuJunkEntries  = wrap(filterJunk(eaeu),  "ЕАЭС");
        List<Entry> allEntries       = wrapAll(rf, eaeu);

        // 3. Пишем XLSX
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            if (Boolean.TRUE.equals(req.getSplitSheets())) {
                writeSheet(book, "Возможно валидные",
                        concat(rfValidEntries, eaeuValidEntries));
                writeSheet(book, "Мусор",
                        concat(rfJunkEntries, eaeuJunkEntries));
                writeSheet(book, "Все подряд",
                        allEntries);
            } else {
                writeSheet(book, "Все", allEntries);
            }

            Path out = Path.of(req.getExcelFilePath());
            if (out.getParent() != null) {
                Files.createDirectories(out.getParent());
            }
            try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
                book.write(fos);
            }
        }

        NotFoundExportResult result = new NotFoundExportResult();
        result.setRfTotal(rf.size());
        result.setEaeuTotal(eaeu.size());
        result.setRfValid(rfValidEntries.size());
        result.setEaeuValid(eaeuValidEntries.size());
        result.setRfJunk(rfJunkEntries.size());
        result.setEaeuJunk(eaeuJunkEntries.size());
        result.setExcelPath(req.getExcelFilePath());
        return result;
    }

    /* ===================== filters ===================== */

    private List<String> filterValid(List<String> list) {
        return list.stream()
                .filter(s -> s != null && !JUNK_PATTERN.matcher(s.trim()).matches())
                .toList();
    }

    private List<String> filterJunk(List<String> list) {
        return list.stream()
                .filter(s -> s != null && JUNK_PATTERN.matcher(s.trim()).matches())
                .toList();
    }

    /* ===================== wrappers ===================== */

    private List<Entry> wrap(List<String> list, String source) {
        List<Entry> out = new ArrayList<>(list.size());
        for (String s : list) {
            if (s != null) out.add(new Entry(s, source));
        }
        return out;
    }

    private List<Entry> wrapAll(List<String> rf, List<String> eaeu) {
        List<Entry> out = new ArrayList<>(rf.size() + eaeu.size());
        for (String s : rf)   if (s != null) out.add(new Entry(s, "RF"));
        for (String s : eaeu) if (s != null) out.add(new Entry(s, "ЕАЭС"));
        return out;
    }

    private List<Entry> concat(List<Entry> a, List<Entry> b) {
        List<Entry> out = new ArrayList<>(a.size() + b.size());
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    /* ===================== xlsx writer ===================== */

    private void writeSheet(XSSFWorkbook book, String name, List<Entry> entries) {
        Sheet sheet = book.createSheet(name);

        // Заголовок
        CellStyle headerStyle = createHeaderStyle(book);
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("№");
        header.createCell(1).setCellValue("Номер сертификата");
        header.createCell(2).setCellValue("Источник");
        for (int i = 0; i <= 2; i++) header.getCell(i).setCellStyle(headerStyle);

        // Данные
        int rowIdx = 1;
        for (Entry e : entries) {
            Row row = sheet.createRow(rowIdx);
            row.createCell(0).setCellValue(rowIdx);
            row.createCell(1).setCellValue(e.number());
            row.createCell(2).setCellValue(e.source());
            rowIdx++;
        }

        // Ширина
        sheet.setColumnWidth(0, 1500);
        sheet.setColumnWidth(1, 12000);
        sheet.setColumnWidth(2, 3000);

        // Заморозка заголовка
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