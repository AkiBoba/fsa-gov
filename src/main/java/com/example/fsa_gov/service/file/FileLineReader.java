package com.example.fsa_gov.service.file;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Универсальное чтение строк из файла: .xlsx (Apache POI) или .txt/.csv.
 */
@Slf4j
@Component
public class FileLineReader {

    public List<String> readLines(String filePath, int columnIndex) throws IOException {
        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            throw new IOException("Файл не найден: " + filePath);
        }
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return readXlsx(path, columnIndex);
        }
        return readPlainText(path);
    }

    /* ================= XLSX ================= */

    private List<String> readXlsx(Path path, int columnIndex) throws IOException {
        List<String> lines = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(path.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter fmt = new DataFormatter();

            for (Row row : sheet) {
                Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                if (cell == null) continue;

                String value = fmt.formatCellValue(cell);
                if (value != null && !value.isBlank()) {
                    lines.add(value.trim());
                }
            }
        }
        log.info("Прочитано строк из xlsx: {}", lines.size());
        return lines;
    }

    /* ================= TXT / CSV ================= */

    private List<String> readPlainText(Path path) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line != null && !line.isBlank()) {
                lines.add(line.trim());
            }
        }
        log.info("Прочитано строк из txt: {}", lines.size());
        return lines;
    }
}