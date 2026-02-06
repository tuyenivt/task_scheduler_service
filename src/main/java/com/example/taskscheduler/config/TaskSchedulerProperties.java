package com.example.taskscheduler.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the task scheduler.
 * Loaded from application.yml.
 */
@Data
@Validated
@Configuration
@ConfigurationProperties(prefix = "task-scheduler")
public class TaskSchedulerProperties {

    /**
     * Polling interval in milliseconds for checking pending tasks
     */
    @Min(1000)
    private long pollIntervalMs = 30000;

    /**
     * Maximum number of tasks to fetch per poll cycle
     */
    @Min(1)
    private int batchSize = 100;

    /**
     * Number of concurrent executor threads
     */
    @Min(1)
    private int executorPoolSize = 20;

    /**
     * Default maximum retry attempts
     */
    @Min(0)
    private int defaultMaxRetries = 5;

    /**
     * Default hours to wait before retry (24 = next day)
     */
    @Min(1)
    private int defaultRetryDelayHours = 24;

    /**
     * Lock duration in minutes
     */
    @Min(1)
    private int lockDurationMinutes = 30;

    /**
     * Threshold in minutes after which a locked task is considered stale
     */
    @Min(1)
    private int staleTaskThresholdMinutes = 60;
}
