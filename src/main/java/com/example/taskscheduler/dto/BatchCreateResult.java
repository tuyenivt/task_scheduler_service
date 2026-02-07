package com.example.taskscheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of a batch task creation operation.
 * Includes both successfully created tasks and per-item errors.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateResult {

    private List<TaskResponse> created;
    private List<BatchItemError> errors;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchItemError {
        private int index;
        private String referenceId;
        private String error;
    }
}
