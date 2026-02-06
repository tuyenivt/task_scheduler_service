package com.example.taskscheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Statistics response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatistics {

    private Map<String, Long> statusDistribution;
    private Map<String, Map<String, Long>> typeStatusDistribution;
    private long pendingCount;
    private long processingCount;
    private long failedCount;
    private long completedCount;
    private Instant generatedAt;
}
