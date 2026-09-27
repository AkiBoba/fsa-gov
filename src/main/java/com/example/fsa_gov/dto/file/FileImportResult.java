package com.example.fsa_gov.dto.file;

import com.example.fsa_gov.dto.CertificateResponseDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Итог импорта из файла: накопленные списки + пути к результирующим файлам.
 */
@Data
public class FileImportResult {

    private int totalLines;
    private int totalBatches;

    /** Был ли включён режим нормализации. */
    private boolean normalizeEnabled;

    /** Сколько номеров реально изменилось в процессе нормализации. */
    private int totalNormalized;

    /** Все найденные документы — и РФ, и ЕАЭС в одном списке.
     *  Различить можно по полю objectId:
     *   - objectId != null → ЕАЭС
     *   - иначе id != null → РФ
     */
    private List<CertificateResponseDto> found = new ArrayList<>();

    private List<String> notFound    = new ArrayList<>();
    private List<String> invalidEntries = new ArrayList<>();

    /** Куда записали результаты */
    private String foundFile;
    private String notFoundFile;

    public int getFoundCount()     { return found.size(); }
    public int getNotFoundCount()  { return notFound.size(); }
    public int getInvalidCount()   { return invalidEntries.size(); }
}