package com.example.taskscheduler.exception;

import lombok.Getter;

/**
 * Exception for duplicate task
 */
@Getter
public class DuplicateTaskException extends RuntimeException {

    private final String referenceId;
    private final String taskType;

    public DuplicateTaskException(String referenceId, String taskType) {
        super(String.format("Active task already exists for reference %s with type %s", referenceId, taskType));
        this.referenceId = referenceId;
        this.taskType = taskType;
    }
}
