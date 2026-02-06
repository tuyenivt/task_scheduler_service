package com.example.taskscheduler.domain.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TaskStatus Enum Tests")
class TaskStatusTest {

    @Test
    @DisplayName("Terminal states should be identified correctly")
    void terminalStatesShouldBeIdentified() {
        assertThat(TaskStatus.COMPLETED.isTerminal()).isTrue();
        assertThat(TaskStatus.CANCELLED.isTerminal()).isTrue();
        assertThat(TaskStatus.EXPIRED.isTerminal()).isTrue();
        assertThat(TaskStatus.MAX_RETRIES_EXCEEDED.isTerminal()).isTrue();
        assertThat(TaskStatus.DEAD_LETTER.isTerminal()).isTrue();

        assertThat(TaskStatus.PENDING.isTerminal()).isFalse();
        assertThat(TaskStatus.PROCESSING.isTerminal()).isFalse();
        assertThat(TaskStatus.FAILED.isTerminal()).isFalse();
        assertThat(TaskStatus.PAUSED.isTerminal()).isFalse();
        assertThat(TaskStatus.RETRY_PENDING.isTerminal()).isFalse();
    }

    @Test
    @DisplayName("Executable states should be identified correctly")
    void executableStatesShouldBeIdentified() {
        assertThat(TaskStatus.PENDING.isExecutable()).isTrue();
        assertThat(TaskStatus.SCHEDULED.isExecutable()).isTrue();
        assertThat(TaskStatus.FAILED.isExecutable()).isTrue();
        assertThat(TaskStatus.RETRY_PENDING.isExecutable()).isTrue();

        assertThat(TaskStatus.PROCESSING.isExecutable()).isFalse();
        assertThat(TaskStatus.COMPLETED.isExecutable()).isFalse();
        assertThat(TaskStatus.CANCELLED.isExecutable()).isFalse();
        assertThat(TaskStatus.PAUSED.isExecutable()).isFalse();
    }

    @Test
    @DisplayName("Failure states should be identified correctly")
    void failureStatesShouldBeIdentified() {
        assertThat(TaskStatus.FAILED.isFailure()).isTrue();
        assertThat(TaskStatus.MAX_RETRIES_EXCEEDED.isFailure()).isTrue();
        assertThat(TaskStatus.DEAD_LETTER.isFailure()).isTrue();

        assertThat(TaskStatus.PENDING.isFailure()).isFalse();
        assertThat(TaskStatus.COMPLETED.isFailure()).isFalse();
    }

    @Test
    @DisplayName("Should lookup by code")
    void shouldLookupByCode() {
        assertThat(TaskStatus.fromCode("pending")).isEqualTo(TaskStatus.PENDING);
        assertThat(TaskStatus.fromCode("retry-pending")).isEqualTo(TaskStatus.RETRY_PENDING);
        assertThat(TaskStatus.fromCode("dead-letter")).isEqualTo(TaskStatus.DEAD_LETTER);
    }

    @Test
    @DisplayName("Should throw for unknown code")
    void shouldThrowForUnknownCode() {
        assertThatThrownBy(() -> TaskStatus.fromCode("nonexistent")).isInstanceOf(IllegalArgumentException.class);
    }
}
