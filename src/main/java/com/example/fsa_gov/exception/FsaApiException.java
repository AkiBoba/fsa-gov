package com.example.fsa_gov.exception;

/**
 * Исключение для ошибок API ФСА.
 */
public class FsaApiException extends RuntimeException {

    private final int statusCode;
    private String errorCode;
    private String detailMessage;
    private String instanceUrl;

    public FsaApiException(int statusCode) {
        super("API Error: " + statusCode);
        this.statusCode = statusCode;
    }

    public FsaApiException(int statusCode, String errorCode, String detailMessage, String instanceUrl) {
        super(detailMessage != null ? detailMessage : "Ошибка API ФСА");
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.detailMessage = detailMessage;
        this.instanceUrl = instanceUrl;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

}
