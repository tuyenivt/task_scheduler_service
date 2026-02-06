package com.example.taskscheduler.dto;

import com.example.taskscheduler.domain.enums.TaskPriority;
import com.example.taskscheduler.domain.enums.TaskStatus;
import com.example.taskscheduler.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Search criteria for filtering tasks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSearchCriteria {

    private TaskType taskType;
    private TaskStatus status;
    private String referenceId;
    private TaskPriority priority;
    private Instant scheduledFrom;
    private Instant scheduledTo;
    private Instant createdFrom;
    private Instant createdTo;
    private String createdBy;
    private Boolean includeExpired;
}
