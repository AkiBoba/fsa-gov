package com.example.fsa_gov.dto.notFoundToExcel;

import lombok.Data;

@Data
public class NotFoundExportResult {
    private int rfTotal;
    private int eaeuTotal;
    private int rfValid;
    private int eaeuValid;
    private int rfJunk;
    private int eaeuJunk;
    private String excelPath;
}