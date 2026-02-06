package com.example.taskscheduler.client;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Request/Response DTOs for external service clients
 */
public class ClientModels {
    private ClientModels() {
    }

    // === Order Service Models ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCancelRequest {
        private String orderId;
        private String reason;
        private String cancelledBy;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderCancelResponse {
        private String orderId;
        private String status;
        private String message;
        private String cancelledAt;
    }

    // === Payment Service Models ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRefundRequest {
        private String paymentId;
        private String transactionId;
        private BigDecimal amount;
        private String currency;
        private String reason;
        private String requestedBy;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRefundResponse {
        private String refundId;
        private String paymentId;
        private String status;
        private BigDecimal amount;
        private String message;
        private String processedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentVoidRequest {
        private String paymentId;
        private String authorizationId;
        private String reason;
        private String requestedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentVoidResponse {
        private String paymentId;
        private String status;
        private String message;
        private String voidedAt;
    }

    // === Generic Response ===

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private boolean success;
        private T data;
        private String errorCode;
        private String errorMessage;
    }
}
