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

    private List<CertificateResponseDto> rfFound   = new ArrayList<>();
    private List<CertificateResponseDto> eaeuFound = new ArrayList<>();

    private List<String> rfNotFound    = new ArrayList<>();
    private List<String> eaeuNotFound  = new ArrayList<>();
    private List<String> invalidEntries = new ArrayList<>();

    /** Куда записали результаты */
    private String rfFoundFile;
    private String eaeuFoundFile;
    private String notFoundFile;

    public int getRfFoundCount()     { return rfFound.size(); }
    public int getEaeuFoundCount()   { return eaeuFound.size(); }
    public int getRfNotFoundCount()  { return rfNotFound.size(); }
    public int getEaeuNotFoundCount(){ return eaeuNotFound.size(); }
    public int getInvalidCount()     { return invalidEntries.size(); }
}