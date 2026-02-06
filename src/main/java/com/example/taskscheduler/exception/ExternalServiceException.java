package com.example.taskscheduler.exception;

import lombok.Getter;

/**
 * Exception for external service communication failures
 */
@Getter
public class ExternalServiceException extends RuntimeException {

    private final String serviceName;
    private final Integer httpStatusCode;
    private final String responseBody;
    private final boolean retryable;

    public ExternalServiceException(String serviceName, String message) {
        super(String.format("[%s] %s", serviceName, message));
        this.serviceName = serviceName;
        this.httpStatusCode = null;
        this.responseBody = null;
        this.retryable = true;
    }

    public ExternalServiceException(String serviceName, Exception cause) {
        super(String.format("[%s] %s", serviceName, cause.getMessage()), cause);
        this.serviceName = serviceName;
        this.httpStatusCode = null;
        this.responseBody = null;
        this.retryable = true;
    }

    public ExternalServiceException(String serviceName, String message, Exception cause) {
        super(String.format("[%s] %s", serviceName, message), cause);
        this.serviceName = serviceName;
        this.httpStatusCode = null;
        this.responseBody = null;
        this.retryable = true;
    }

    public ExternalServiceException(String serviceName, int httpStatusCode, String responseBody) {
        super(String.format("[%s] HTTP %d: %s", serviceName, httpStatusCode, responseBody));
        this.serviceName = serviceName;
        this.httpStatusCode = httpStatusCode;
        this.responseBody = responseBody;
        // 4xx errors (except 408, 429) are not retryable
        this.retryable = httpStatusCode >= 500 || httpStatusCode == 408 || httpStatusCode == 429;
    }
}
