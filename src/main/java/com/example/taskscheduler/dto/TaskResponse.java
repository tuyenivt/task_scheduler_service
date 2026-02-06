package com.example.taskscheduler.dto;

import com.example.taskscheduler.domain.enums.TaskPriority;
import com.example.taskscheduler.domain.enums.TaskStatus;
import com.example.taskscheduler.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for task data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {

    private UUID id;
    private TaskType taskType;
    private TaskStatus status;
    private TaskPriority priority;
    private String referenceId;
    private String secondaryReferenceId;
    private String description;
    private Map<String, Object> payload;
    private Map<String, Object> metadata;
    private Instant scheduledTime;
    private Instant expiresAt;
    private Integer retryCount;
    private Integer maxRetries;
    private Integer retryDelayHours;
    private String cronExpression;
    private String lastError;
    private Map<String, Object> executionResult;
    private String lockedBy;
    private Instant lockedUntil;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private Instant startedAt;
    private Instant completedAt;
    private Long executionDurationMs;

    /**
     * Execution history (populated on detail requests)
     */
    private List<TaskExecutionLogResponse> executionHistory;
}