package com.example.fsa_gov.dto.notFoundToExcel;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на экспорт result-not-found.json в Excel")
public class NotFoundToExcelRequest {

    @NotBlank
    @Schema(description = "Путь к JSON-файлу",
            example = "D:/files/output/result-not-found.json")
    private String jsonFilePath;

    @NotBlank
    @Schema(description = "Путь для сохранения Excel",
            example = "D:/files/output/result-not-found.xlsx")
    private String excelFilePath;

    @Schema(description = "Разделить листы: valid / junk / all",
            defaultValue = "true")
    private Boolean splitSheets = true;
}