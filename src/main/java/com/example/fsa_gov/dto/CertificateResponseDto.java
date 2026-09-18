package com.example.fsa_gov.dto;

import lombok.Data;
import java.util.List;
import java.util.Optional;

/**
 * DTO для полного ответа API ФСА о сертификате соответствия РФ (СС).
 */
@Data
public class CertificateResponseDto {

    /** Идентификатор документа во ФГИС Росаккредитация. */
    private Long id;

    /** Номер разрешительного документа. */
    private String numberDoc;

    /** Дата регистрации в формате YYYY-MM-DD. */
    private String regDate;

    /** Дата истечения срока действия (nullable). */
    private Optional<String> endDate = Optional.empty();

    /** Статус документа: 6=действует, 14=прекращен, 15=приостановлен и т.д. */
    private Integer idStatus;

    /** Тип документа: 2=декларация ЕАЭС, 11=сертификат РФ и др. */
    private Integer idDocType;

    /** Список продукции в документе (nullable). */
    private List<ProductItemDto> products;

    /** Список технических регламентов (nullable). */
    private List<String> techRegs;

    /** Список групп продукции (nullable). */
    private List<String> productGroups;

    /** Единый перечень продукции (nullable). */
    private Optional<List<String>> productSingleLists = Optional.empty();

    /** Наименование изготовителя. */
    private String manufacturerName;

    /** Адрес изготовителя. */
    private String manufacturerAddress;

    /** Список адресов филиалов/деятельности (nullable). */
    private List<String> manufacturerFilialsAddress;

    /** Идентификатор заменённого документа (nullable). */
    private Optional<Long> idDocReplaced = Optional.empty();

    /** Идентификатор выданного взамен этого документа (nullable). */
    private Optional<Long> idDocRegInstead = Optional.empty();

    /** Дата последнего обновления в формате ISO 8601. */
    private String updated;

}
