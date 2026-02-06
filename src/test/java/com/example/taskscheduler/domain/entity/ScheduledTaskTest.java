package com.example.taskscheduler.domain.entity;

import com.example.taskscheduler.domain.enums.TaskPriority;
import com.example.taskscheduler.domain.enums.TaskStatus;
import com.example.taskscheduler.domain.enums.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ScheduledTask Entity Tests")
class ScheduledTaskTest {

    private ScheduledTask task;

    @BeforeEach
    void setUp() {
        task = ScheduledTask.builder()
                .taskType(TaskType.ORDER_CANCEL)
                .status(TaskStatus.PENDING)
                .priority(TaskPriority.NORMAL)
                .referenceId("ORD-12345")
                .scheduledTime(Instant.now())
                .retryCount(0)
                .build();
    }

    @Nested
    @DisplayName("isLocked Tests")
    class IsLockedTests {

        @Test
        @DisplayName("Should return false when not locked")
        void shouldReturnFalseWhenNotLocked() {
            assertThat(task.isLocked()).isFalse();
        }

        @Test
        @DisplayName("Should return true when locked with future expiry")
        void shouldReturnTrueWhenLockedWithFutureExpiry() {
            task.setLockedBy("instance-1");
            task.setLockedUntil(Instant.now().plusSeconds(3600));
            assertThat(task.isLocked()).isTrue();
        }

        @Test
        @DisplayName("Should return false when lock has expired")
        void shouldReturnFalseWhenLockExpired() {
            task.setLockedBy("instance-1");
            task.setLockedUntil(Instant.now().minusSeconds(60));
            assertThat(task.isLocked()).isFalse();
        }

        @Test
        @DisplayName("Should return false when lockedBy is null")
        void shouldReturnFalseWhenLockedByNull() {
            task.setLockedBy(null);
            task.setLockedUntil(Instant.now().plusSeconds(3600));
            assertThat(task.isLocked()).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpired Tests")
    class IsExpiredTests {

        @Test
        @DisplayName("Should return false when no expiry set")
        void shouldReturnFalseWhenNoExpiry() {
            assertThat(task.isExpired()).isFalse();
        }

        @Test
        @DisplayName("Should return true when expired")
        void shouldReturnTrueWhenExpired() {
            task.setExpiresAt(Instant.now().minusSeconds(60));
            assertThat(task.isExpired()).isTrue();
        }

        @Test
        @DisplayName("Should return false when not yet expired")
        void shouldReturnFalseWhenNotYetExpired() {
            task.setExpiresAt(Instant.now().plusSeconds(3600));
            assertThat(task.isExpired()).isFalse();
        }
    }

    @Nested
    @DisplayName("Retry Configuration Tests")
    class RetryConfigTests {

        @Test
        @DisplayName("Should use task-specific max retries when set")
        void shouldUseTaskSpecificMaxRetries() {
            task.setMaxRetries(10);
            assertThat(task.getEffectiveMaxRetries(5)).isEqualTo(10);
        }

        @Test
        @DisplayName("Should use default max retries when not set")
        void shouldUseDefaultMaxRetries() {
            assertThat(task.getEffectiveMaxRetries(5)).isEqualTo(5);
        }

        @Test
        @DisplayName("Should check canRetry correctly")
        void shouldCheckCanRetryCorrectly() {
            task.setRetryCount(3);
            assertThat(task.canRetry(5)).isTrue();
            assertThat(task.canRetry(3)).isFalse();
        }

        @Test
        @DisplayName("Should use task-specific retry delay when set")
        void shouldUseTaskSpecificRetryDelay() {
            task.setRetryDelayHours(2);
            assertThat(task.getEffectiveRetryDelayHours(24)).isEqualTo(2);
        }

        @Test
        @DisplayName("Should use default retry delay when not set")
        void shouldUseDefaultRetryDelay() {
            assertThat(task.getEffectiveRetryDelayHours(24)).isEqualTo(24);
        }
    }

    @Nested
    @DisplayName("Metadata and Payload Tests")
    class MetadataPayloadTests {

        @Test
        @DisplayName("Should add and retrieve metadata")
        void shouldAddAndRetrieveMetadata() {
            task.putMetadata("key", "value");
            assertThat(task.getMetadataValue("key", String.class)).isEqualTo("value");
        }

        @Test
        @DisplayName("Should return null for missing metadata key")
        void shouldReturnNullForMissingKey() {
            assertThat(task.getMetadataValue("nonexistent", String.class)).isNull();
        }

        @Test
        @DisplayName("Should return null for wrong type")
        void shouldReturnNullForWrongType() {
            task.putMetadata("key", "value");
            assertThat(task.getMetadataValue("key", Integer.class)).isNull();
        }

        @Test
        @DisplayName("Should add and retrieve payload")
        void shouldAddAndRetrievePayload() {
            task.putPayload("amount", 100.50);
            assertThat(task.getPayloadValue("amount", Double.class)).isEqualTo(100.50);
        }

        @Test
        @DisplayName("Should initialize metadata map when null")
        void shouldInitializeMetadataWhenNull() {
            task = ScheduledTask.builder()
                    .taskType(TaskType.ORDER_CANCEL)
                    .status(TaskStatus.PENDING)
                    .referenceId("test")
                    .scheduledTime(Instant.now())
                    .build();
            // Default metadata is HashMap from @Builder.Default, but test with explicit null
            task.setMetadata(null);
            task.putMetadata("key", "value");
            assertThat(task.getMetadata()).containsEntry("key", "value");
        }
    }

    @Nested
    @DisplayName("Lifecycle Callback Tests")
    class LifecycleCallbackTests {

        @Test
        @DisplayName("Should set audit fields on create")
        void shouldSetAuditFieldsOnCreate() {
            var newTask = new ScheduledTask();
            newTask.onCreate();
            assertThat(newTask.getCreatedAt()).isNotNull();
            assertThat(newTask.getUpdatedAt()).isNotNull();
            assertThat(newTask.getStatus()).isEqualTo(TaskStatus.PENDING);
            assertThat(newTask.getPriority()).isEqualTo(TaskPriority.NORMAL);
            assertThat(newTask.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("Should update updatedAt on update")
        void shouldUpdateUpdatedAtOnUpdate() {
            task.setCreatedAt(Instant.now().minusSeconds(3600));
            task.setUpdatedAt(Instant.now().minusSeconds(3600));
            var before = task.getUpdatedAt();
            task.onUpdate();
            assertThat(task.getUpdatedAt()).isAfterOrEqualTo(before);
        }
    }
}
