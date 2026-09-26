package com.example.fsa_gov.dto.notFoundToExcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotFoundFileDto {
    private Integer rfNotFoundCount;
    private Integer eaeuNotFoundCount;
    private Integer invalidCount;
    private List<String> rfNotFound;
    private List<String> eaeuNotFound;
    private List<String> invalidEntries;
}