package com.example.taskscheduler.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request for bulk operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkTaskRequest {

    @NotEmpty(message = "Task IDs are required")
    @Size(max = 100, message = "Maximum 100 tasks per batch")
    private List<UUID> taskIds;

    private String reason;
}
