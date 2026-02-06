package com.example.taskscheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request for retrying a task
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryTaskRequest {

    private Instant scheduledTime;
    private boolean immediate;
}
