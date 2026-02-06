package com.example.taskscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Task Scheduler Service Application
 * <p>
 * A high-volume, distributed task scheduler for back-office operations
 * such as order cancellation and payment refund processing.
 * <p>
 * Features:
 * - Virtual Thread support for high concurrency
 * - Distributed task locking to prevent duplicate execution in EKS
 * - Configurable retry logic with next-day default
 * - Slack alerting for max retries reached
 * - Full task lifecycle management
 */
@EnableScheduling
@SpringBootApplication
public class TaskSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskSchedulerApplication.class, args);
    }
}
