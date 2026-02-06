package com.example.taskscheduler.dto;

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

    private List<UUID> taskIds;
    private String reason;
}
