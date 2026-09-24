package com.example.fsa_gov.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * DTO для ответа API ФСА о сертификате (СС РФ или ЕАЭС).
 * Поля сделаны устойчивыми к различиям схем РФ/ЕАЭС.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)   // игнорируем новые поля
public class CertificateResponseDto {

    /** ID документа. ФСА отдаёт либо число, либо UUID-строку — поэтому String. */
    private String id;

    /** Внутренний ObjectId (только у ЕАЭС). */
    private String objectId;

    /** Номер документа. */
    private String numberDoc;

    /** Дата регистрации YYYY-MM-DD. */
    private String regDate;

    /** Дата окончания действия (nullable). */
    private String endDate;

    // --- Статусы: РФ и ЕАЭС отдают по-разному ---
    /** РФ: числовой статус (6, 14, 15...). */
    private Integer idStatus;
    /** ЕАЭС: строковый статус ("01", "02"...). */
    private String idStatusEAEU;
    /** РФ: числовой статус в реестре РФ (может отсутствовать у ЕАЭС). */
    private Integer idStatusInRF;

    // --- Типы документов ---
    /** РФ: числовой тип (2, 11...). */
    private Integer idDocType;
    /** ЕАЭС: строковый тип ("05"...). */
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