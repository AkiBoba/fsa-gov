package com.example.fsa_gov.dto.notFoundToExcel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotFoundFileDto {

    // --- Новый формат (один список) ---
    private Integer notFoundCount;
    private List<String> notFound;

    // --- Старый формат (два списка) ---
    private Integer rfNotFoundCount;
    private Integer eaeuNotFoundCount;
    private List<String> rfNotFound;
    private List<String> eaeuNotFound;

    private Integer invalidCount;
    private List<String> invalidEntries;

    /**
     * Возвращает все ненайденные номера независимо от формата файла.
     */
    public List<String> getAllNotFound() {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (notFound     != null) result.addAll(notFound);      // новый формат
        if (rfNotFound   != null) result.addAll(rfNotFound);    // старый формат
        if (eaeuNotFound != null) result.addAll(eaeuNotFound);  // старый формат
        return result;
    }
}