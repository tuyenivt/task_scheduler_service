package com.example.taskscheduler.exception;

import lombok.Getter;

/**
 * Exception for invalid task state transition
 */
@Getter
public class InvalidTaskStateException extends RuntimeException {

    private final String taskId;
    private final String currentState;
    private final String requestedState;

    public InvalidTaskStateException(String taskId, String currentState, String requestedState) {
        super(String.format("Cannot transition task %s from %s to %s", taskId, currentState, requestedState));
        this.taskId = taskId;
        this.currentState = currentState;
        this.requestedState = requestedState;
    }
}
