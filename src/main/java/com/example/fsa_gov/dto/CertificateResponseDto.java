package com.example.fsa_gov.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO для ответа API ФСА о сертификате (СС РФ или ЕАЭС).
 * Поля сделаны устойчивыми к различиям схем РФ/ЕАЭС.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CertificateResponseDto {

    /** ID документа. ФСА отдаёт либо число, либо UUID-строку — поэтому String. */
    private String id;

    /** Внутренний ObjectId (только у ЕАЭС). */
    private String objectId;

    /** Номер документа (как хранит ФСА — канонический). */
    private String numberDoc;

    /**
     * Исходный номер, который искали.
     *
     * Заполняется всегда:
     *  - при обычном флоу: sourceNumber == numberDoc;
     *  - при нормализации: sourceNumber = номер ДО нормализации,
     *    numberDoc = номер ПОСЛЕ (как вернул ФСА).
     */
    private String sourceNumber;

    /** Дата регистрации YYYY-MM-DD. */
    private String regDate;

    /** Дата окончания действия (nullable). */
    private String endDate;

    // --- Статусы: РФ и ЕАЭС отдают по-разному ---
    private Integer idStatus;
    private String idStatusEAEU;
    private Integer idStatusInRF;

    // --- Типы документов ---
    private Integer idDocType;
    private String idDocTypeEAEU;

    // --- Продукция и регламенты ---
    private List<ProductItemDto> products;
    private List<String> techRegs;
    private List<String> productGroups;
    private List<String> productSingleLists;

    // --- Изготовитель ---
    private String manufacturerName;
    private String manufacturerAddress;
    private List<String> manufacturerFilialsAddress;

    // --- Связи (только РФ) ---
    private Long idDocReplaced;
    private Long idDocRegInstead;

    /** Дата обновления ISO 8601. */
    private String updated;
}