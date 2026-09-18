package com.example.fsa_gov.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO для запроса получения информации о сертификате соответствия РФ (СС)
 */
@Data
public class CertificateRequestDto {

    /**
     * Номер разрешительного документа (обязательное поле).
     * Пример: "РОСС RU С-RU.HB34.B.00690/26" или "ЕАЭС N RU Д-RU.PA01.B.05217/26"
     */
    @NotBlank(message = "Номер документа обязателен")
    private String numberDoc;

    /**
     * Дата регистрации документа в формате YYYY-MM-DD (необязательно).
     * Рекомендуется указывать, если по номеру найдено несколько совпадений.
     */
    private String regDate;

    /**
     * ИНН заявителя/владельца документа (10 или 12 цифр) (необязательно).
     * Используется для разрешения неоднозначности при поиске документа.
     */
    private String applicantInn;

}
