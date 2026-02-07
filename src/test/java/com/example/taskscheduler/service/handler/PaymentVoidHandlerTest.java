package com.example.taskscheduler.service.handler;

import com.example.taskscheduler.client.ClientModels.PaymentVoidResponse;
import com.example.taskscheduler.client.PaymentServiceClient;
import com.example.taskscheduler.domain.entity.ScheduledTask;
import com.example.taskscheduler.domain.enums.TaskPriority;
import com.example.taskscheduler.domain.enums.TaskStatus;
import com.example.taskscheduler.domain.enums.TaskType;
import com.example.taskscheduler.exception.ExternalServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentVoidHandler Tests")
class PaymentVoidHandlerTest {

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @InjectMocks
    private PaymentVoidHandler handler;

    @Test
    @DisplayName("Should return PAYMENT_VOID task type")
    void shouldReturnCorrectTaskType() {
        assertThat(handler.getTaskType()).isEqualTo(TaskType.PAYMENT_VOID);
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should pass validation with valid referenceId")
        void shouldPassValidation() {
            var task = buildTask("PAY-123");
            handler.validate(task);
        }

        @Test
        @DisplayName("Should reject null referenceId")
        void shouldRejectNullReferenceId() {
            var task = buildTask(null);
            assertThatThrownBy(() -> handler.validate(task))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should reject blank referenceId")
        void shouldRejectBlankReferenceId() {
            var task = buildTask("  ");
            assertThatThrownBy(() -> handler.validate(task))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {

        @Test
        @DisplayName("Should return success when payment voided")
        void shouldReturnSuccessWhenPaymentVoided() {
            var task = buildTask("PAY-123");
            when(paymentServiceClient.voidPayment(any())).thenReturn(
                    PaymentVoidResponse.builder()
                            .paymentId("PAY-123").status("VOIDED").message("OK")
                            .voidedAt(Instant.now().toString())
                            .build());

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getResponseData()).containsEntry("paymentId", "PAY-123");
            assertThat(result.getResponseData()).containsEntry("status", "VOIDED");
        }

        @Test
        @DisplayName("Should accept SUCCESS status as success")
        void shouldAcceptSuccessStatus() {
            var task = buildTask("PAY-123");
            when(paymentServiceClient.voidPayment(any())).thenReturn(
                    PaymentVoidResponse.builder()
                            .paymentId("PAY-123").status("SUCCESS").message("OK")
                            .build());

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isTrue();
        }

        @Test
        @DisplayName("Should return failure when unexpected response status")
        void shouldReturnFailureWhenUnexpectedStatus() {
            var task = buildTask("PAY-123");
            when(paymentServiceClient.voidPayment(any())).thenReturn(
                    PaymentVoidResponse.builder()
                            .paymentId("PAY-123").status("PENDING").message("Not processed")
                            .build());

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getErrorType()).isEqualTo("UNEXPECTED_STATUS");
        }

        @Test
        @DisplayName("Should return permanent failure when payment not found (404)")
        void shouldReturnPermanentFailureWhenPaymentNotFound() {
            var task = buildTask("PAY-999");
            when(paymentServiceClient.voidPayment(any()))
                    .thenThrow(new ExternalServiceException("Payment Service", 404, "Not found"));

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRetryable()).isFalse();
            assertThat(result.getErrorType()).isEqualTo("PAYMENT_NOT_FOUND");
        }

        @Test
        @DisplayName("Should return permanent failure on conflict (409)")
        void shouldReturnPermanentFailureOnConflict() {
            var task = buildTask("PAY-123");
            when(paymentServiceClient.voidPayment(any()))
                    .thenThrow(new ExternalServiceException("Payment Service", 409, "Already voided"));

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRetryable()).isFalse();
            assertThat(result.getErrorType()).isEqualTo("PAYMENT_STATE_CONFLICT");
        }

        @Test
        @DisplayName("Should return permanent failure on bad request (400)")
        void shouldReturnPermanentFailureOnBadRequest() {
            var task = buildTask("PAY-123");
            when(paymentServiceClient.voidPayment(any()))
                    .thenThrow(new ExternalServiceException("Payment Service", 400, "Invalid request"));

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRetryable()).isFalse();
            assertThat(result.getErrorType()).isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("Should return retryable failure on server error (500)")
        void shouldReturnRetryableFailureOnServerError() {
            var task = buildTask("PAY-123");
            when(paymentServiceClient.voidPayment(any()))
                    .thenThrow(new ExternalServiceException("Payment Service", 500, "Internal error"));

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRetryable()).isTrue();
            assertThat(result.getHttpStatusCode()).isEqualTo(500);
        }

        @Test
        @DisplayName("Should return retryable failure on generic exception")
        void shouldReturnRetryableFailureOnGenericException() {
            var task = buildTask("PAY-123");
            when(paymentServiceClient.voidPayment(any()))
                    .thenThrow(new RuntimeException("Connection refused"));

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.isRetryable()).isTrue();
        }

        @Test
        @DisplayName("Should use default payload values when missing")
        void shouldUseDefaultPayloadValuesWhenMissing() {
            var task = buildTask("PAY-123");
            task.setPayload(null);

            when(paymentServiceClient.voidPayment(any())).thenReturn(
                    PaymentVoidResponse.builder()
                            .paymentId("PAY-123").status("VOIDED").message("OK")
                            .build());

            var result = handler.execute(task);

            assertThat(result.isSuccess()).isTrue();
        }
    }

    private ScheduledTask buildTask(String referenceId) {
        return ScheduledTask.builder()
                .id(UUID.randomUUID())
                .taskType(TaskType.PAYMENT_VOID)
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.NORMAL)
                .referenceId(referenceId)
                .secondaryReferenceId("AUTH-456")
                .payload(new HashMap<>(Map.of("reason", "Automated void", "requestedBy", "system")))
                .metadata(new HashMap<>())
                .retryCount(0)
                .scheduledTime(Instant.now())
                .build();
    }
}
