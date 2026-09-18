package com.example.fsa_gov.dto;

import lombok.Data;
import java.util.List;

/**
 * DTO для объекта продукции внутри разрешительного документа.
 */
@Data
public class ProductItemDto {

    /**
     * Наименование продукции (может содержать кириллицу).
     */
    private String productName;

    /**
     * Список кодов ТН ВЭД, относящихся к продукции (nullable).
     */
    private List<String> tnveds;

    /**
     * Список кодов ОКПД2, относящихся к продукции (nullable).
     */
    private List<String> okpds;

}
