package com.example.taskscheduler.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the types of tasks that can be scheduled and executed.
 * Each task type maps to a specific handler implementation.
 */
@Getter
@RequiredArgsConstructor
public enum TaskType {

    /**
     * Cancel an order in the order service
     */
    ORDER_CANCEL("order-cancel", "Order Cancellation"),

    /**
     * Refund a payment through the payment service
     */
    PAYMENT_REFUND("payment-refund", "Payment Refund"),

    /**
     * Partial refund for a payment
     */
    PAYMENT_PARTIAL_REFUND("payment-partial-refund", "Partial Payment Refund"),

    /**
     * Void a pending payment authorization
     */
    PAYMENT_VOID("payment-void", "Payment Void"),

    /**
     * Generic webhook notification task
     */
    WEBHOOK_NOTIFICATION("webhook-notification", "Webhook Notification"),

    /**
     * Custom task type for extensibility
     */
    CUSTOM("custom", "Custom Task");

    private final String code;
    private final String displayName;

    /**
     * Find TaskType by its code value
     */
    public static TaskType fromCode(String code) {
        for (var type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown task type code: " + code);
    }
}
