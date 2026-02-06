package com.example.taskscheduler.exception;

import lombok.Getter;

/**
 * Exception for task lock acquisition failure
 */
@Getter
public class TaskLockException extends RuntimeException {

    private final String taskId;

    public TaskLockException(String taskId) {
        super("Failed to acquire lock for task: " + taskId);
        this.taskId = taskId;
    }

    public TaskLockException(String taskId, String reason) {
        super(String.format("Failed to acquire lock for task %s: %s", taskId, reason));
        this.taskId = taskId;
    }
}
