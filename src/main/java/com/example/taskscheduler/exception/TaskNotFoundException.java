package com.example.taskscheduler.exception;

import lombok.Getter;

import java.util.UUID;

/**
 * Exception for task not found
 */
@Getter
public class TaskNotFoundException extends RuntimeException {

    private final String taskId;

    public TaskNotFoundException(String taskId) {
        super("Task not found: " + taskId);
        this.taskId = taskId;
    }

    public TaskNotFoundException(UUID taskId) {
        this(taskId.toString());
    }
}
