package com.example.fsa_gov.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для контроллеров.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Обрабатывает ошибки API ФСА (409 Conflict, 404 Not Found и т.д.).
     */
    @ExceptionHandler(FsaApiException.class)
    public ResponseEntity<Map<String, Object>> handleFsaError(FsaApiException ex) {

        // Используем HashMap, а не Map.ofEntries,
        // потому что после создания нужно добавить поле "instance"
        Map<String, Object> error = new HashMap<>();
        error.put("status", ex.getStatusCode());
        error.put("code", ex.getErrorCode() != null ? ex.getErrorCode() : "UNKNOWN_ERROR");
        error.put("detail", ex.getDetailMessage() != null
                ? ex.getDetailMessage()
                : "Ошибка API ФСА");

        if (ex.getInstanceUrl() != null) {
            error.put("instance", ex.getInstanceUrl());
        }

        return ResponseEntity
                .status(HttpStatus.valueOf(ex.getStatusCode()))
                .body(error);
    }
}