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
import org.springframework.web.multipart.MultipartFile;

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

    /** Мусор — Письма, Решения, Паспорта и т.п. */
    private static final Pattern JUNK_PATTERN = Pattern.compile(
            "^(Письмо|ПИСЬМО|Пиьсмо|Пиьсо|Решение|РЕШЕНИЕ|Отказное|ОТКАЗНОЕ|" +
                    "Паспорт|ПАСПОРТ|Уведомление|Удостоверение|Свидетельство|Справка|" +
                    "Сертификат б/н|Сертификат качества|Сертификат на|Сертификат №|" +
                    "тест|СДС|VCS-IST|ПАО ПСМ|\\*Товар без сертификата|" +
                    "Информационное письмо|Отказное решение|Удостоверение о качестве).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** РФ-префикс — чтобы отличить РФ от ЕАЭС. */
    private static final Pattern RF_PREFIX = Pattern.compile(
            "^(РОСС\\s+RU|РОСС\\s+[A-Z]{2}\\.|РООС\\s+RU|ТС\\s+RU|RU\\.\\d|\\d{6}\\/).*",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private record Entry(String number, String source) {}

    public NotFoundExportResult export(MultipartFile file,
                                       String excelFilePath,
                                       boolean splitSheets) throws IOException {

        // 1. Читаем JSON (совместимо со старым и новым форматом)
        NotFoundFileDto json = objectMapper.readValue(
                file.getInputStream(), NotFoundFileDto.class);

        List<String> allNotFound = json.getAllNotFound();
        List<String> invalidEntries = json.getInvalidEntries() != null
                ? json.getInvalidEntries() : List.of();

        // 2. Разделяем по префиксу на РФ / ЕАЭС и фильтруем мусор
        List<Entry> rfValid   = new ArrayList<>();
        List<Entry> eaeuValid = new ArrayList<>();
        List<Entry> rfJunk    = new ArrayList<>();
        List<Entry> eaeuJunk  = new ArrayList<>();

        for (String number : allNotFound) {
            if (number == null) continue;
            boolean isRf = RF_PREFIX.matcher(number).matches();
            boolean isJunk = JUNK_PATTERN.matcher(number.trim()).matches();

            if (isRf) {
                if (isJunk) rfJunk.add(new Entry(number, "RF"));
                else        rfValid.add(new Entry(number, "RF"));
            } else {
                if (isJunk) eaeuJunk.add(new Entry(number, "ЕАЭС"));
                else        eaeuValid.add(new Entry(number, "ЕАЭС"));
            }
        }

        // 3. Пишем XLSX
        try (XSSFWorkbook book = new XSSFWorkbook()) {
            if (splitSheets) {
                writeSheet(book, "Возможно валидные", concat(rfValid, eaeuValid));
                writeSheet(book, "Мусор", concat(rfJunk, eaeuJunk));
                writeSheet(book, "Все подряд", concat(concat(rfValid, eaeuValid), concat(rfJunk, eaeuJunk)));
            } else {
                writeSheet(book, "Все", concat(concat(rfValid, eaeuValid), concat(rfJunk, eaeuJunk)));
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

    /* ===================== helpers ===================== */

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