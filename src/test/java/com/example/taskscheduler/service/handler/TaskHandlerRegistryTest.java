package com.example.taskscheduler.service.handler;

import com.example.taskscheduler.domain.entity.ScheduledTask;
import com.example.taskscheduler.domain.enums.TaskType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaskHandlerRegistry Tests")
class TaskHandlerRegistryTest {

    private TaskHandlerRegistry registry;

    private final TaskHandler orderCancelHandler = new TaskHandler() {
        @Override
        public TaskType getTaskType() {
            return TaskType.ORDER_CANCEL;
        }

        @Override
        public TaskExecutionResult execute(ScheduledTask task) {
            return TaskExecutionResult.success();
        }
    };

    private final TaskHandler paymentRefundHandler = new TaskHandler() {
        @Override
        public TaskType getTaskType() {
            return TaskType.PAYMENT_REFUND;
        }

        @Override
        public TaskExecutionResult execute(ScheduledTask task) {
            return TaskExecutionResult.success();
        }
    };

    @BeforeEach
    void setUp() {
        registry = new TaskHandlerRegistry(List.of(orderCancelHandler, paymentRefundHandler));
        registry.initialize();
    }

    @Test
    @DisplayName("Should register and retrieve handlers")
    void shouldRegisterAndRetrieveHandlers() {
        assertThat(registry.getHandler(TaskType.ORDER_CANCEL)).isPresent();
        assertThat(registry.getHandler(TaskType.ORDER_CANCEL).get()).isSameAs(orderCancelHandler);
        assertThat(registry.getHandler(TaskType.PAYMENT_REFUND)).isPresent();
    }

    @Test
    @DisplayName("Should return empty for unregistered type")
    void shouldReturnEmptyForUnregisteredType() {
        assertThat(registry.getHandler(TaskType.CUSTOM)).isEmpty();
    }

    @Test
    @DisplayName("Should throw for unregistered type when using getHandlerOrThrow")
    void shouldThrowForUnregisteredType() {
        assertThatThrownBy(() -> registry.getHandlerOrThrow(TaskType.CUSTOM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No handler registered");
    }

    @Test
    @DisplayName("Should report correct handler count")
    void shouldReportCorrectHandlerCount() {
        assertThat(registry.getHandlerCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check handler existence")
    void shouldCheckHandlerExistence() {
        assertThat(registry.hasHandler(TaskType.ORDER_CANCEL)).isTrue();
        assertThat(registry.hasHandler(TaskType.CUSTOM)).isFalse();
    }

    @Test
    @DisplayName("Should return registered types")
    void shouldReturnRegisteredTypes() {
        assertThat(registry.getRegisteredTypes())
                .containsExactlyInAnyOrder(TaskType.ORDER_CANCEL, TaskType.PAYMENT_REFUND);
    }
}
