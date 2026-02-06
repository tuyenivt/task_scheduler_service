package com.example.taskscheduler.dto;

import com.example.taskscheduler.domain.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for execution log
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionLogResponse {

    private UUID id;
    private UUID taskId;
    private Integer attemptNumber;
    private TaskStatus status;
    private String executorInstance;
    private Instant startedAt;
    private Instant completedAt;
    private Long durationMs;
    private Boolean success;
    private String errorMessage;
    private String errorType;
    private Integer httpStatusCode;
    private Map<String, Object> requestPayload;
    private Map<String, Object> responsePayload;
    private String notes;
    private Instant createdAt;
}
