package com.example.fsa_gov.response;

import lombok.Data;

/**
 * DTO для структурированной ошибки API ФСА.
 */
@Data
public class FsaApiErrorResponse {

    private Integer status;           // HTTP-статус (401, 403, 409 и т.д.)
    private String title;             // Краткое описание статуса на английском ("Not Found", "Conflict")
    private String detail;            // Подробное описание ошибки на русском языке
    private String description;       // Развернутое сообщение с пояснением причины (nullable)
    private String code;              // Внутренний код сервиса ФСА ("MRKTPLS-APP-NOT_FOUND" и т.д.)
    private String instance;          // Путь к эндпоинту, где произошла ошибка

}
