package com.example.fsa_gov.dto.file;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Запрос на импорт сертификатов из файла")
public class FileImportRequest {

    @NotBlank
    @Schema(description = "Путь к файлу (.xlsx или .txt)",
            example = "D:/files/certificates.xlsx")
    private String filePath;

    @Schema(description = "Индекс колонки с номерами (только для xlsx, 0-based)",
            example = "0", defaultValue = "0")
    private Integer columnIndex = 0;

    @Schema(description = "Размер батча (по умолчанию 1000)",
            example = "1000", defaultValue = "1000")
    private Integer batchSize = 1000;

    @Schema(description = "Папка для выходных файлов",
            example = "D:/files/output", defaultValue = "./output")
    private String outputDir = "./output";
}