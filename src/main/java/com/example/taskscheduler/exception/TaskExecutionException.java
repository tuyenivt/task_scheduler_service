package com.example.taskscheduler.exception;

import lombok.Getter;

/**
 * Exception for task execution failures
 */
@Getter
public class TaskExecutionException extends RuntimeException {

    private final String taskId;
    private final boolean retryable;

    public TaskExecutionException(String taskId, String message, boolean retryable) {
        super(String.format("Task %s execution failed: %s", taskId, message));
        this.taskId = taskId;
        this.retryable = retryable;
    }

    public TaskExecutionException(String taskId, Exception cause, boolean retryable) {
        super(String.format("Task %s execution failed: %s", taskId, cause.getMessage()), cause);
        this.taskId = taskId;
        this.retryable = retryable;
    }
}
