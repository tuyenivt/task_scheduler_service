package com.example.taskscheduler.service.executor;

import com.example.taskscheduler.config.MetricsConfig;
import com.example.taskscheduler.config.TaskSchedulerProperties;
import com.example.taskscheduler.domain.entity.ScheduledTask;
import com.example.taskscheduler.domain.entity.TaskExecutionLog;
import com.example.taskscheduler.domain.enums.TaskStatus;
import com.example.taskscheduler.domain.repository.ScheduledTaskRepository;
import com.example.taskscheduler.domain.repository.TaskExecutionLogRepository;
import com.example.taskscheduler.service.alert.SlackAlertService;
import com.example.taskscheduler.service.handler.TaskExecutionResult;
import com.example.taskscheduler.service.handler.TaskHandlerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for executing individual tasks.
 * <p>
 * Handles:
 * - Task lock acquisition and release
 * - Handler invocation
 * - Result processing and status updates
 * - Retry scheduling
 * - Execution logging
 * - Metrics recording
 * - Alert triggering for max retries
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutorService {

    private final ScheduledTaskRepository taskRepository;
    private final TaskExecutionLogRepository executionLogRepository;
    private final TaskHandlerRegistry handlerRegistry;
    private final SlackAlertService slackAlertService;
    private final MetricsConfig metricsConfig;
    private final TaskSchedulerProperties properties;

    @Value("${HOSTNAME:unknown}")
    private String hostname;

    private String instanceId;

    /**
     * Get unique instance ID for this service instance
     */
    private String getInstanceId() {
        if (instanceId == null) {
            try {
                var host = InetAddress.getLocalHost().getHostName();
                instanceId = host + "-" + ProcessHandle.current().pid();
            } catch (Exception e) {
                instanceId = hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
            }
        }
        return instanceId;
    }

    /**
     * Execute a single task with full lifecycle management.
     *
     * @param task The task to execute
     * @return true if execution was successful
     */
    @Transactional
    public boolean executeTask(ScheduledTask task) {
        var taskId = task.getId().toString();

        // Re-fetch task within this transaction to get current state after lock acquisition
        var freshTask = taskRepository.findById(task.getId()).orElse(null);
        if (freshTask == null) {
            log.warn("Task {} no longer exists", taskId);
            return false;
        }
        task = freshTask;

        log.info("Starting execution of task {} (type: {}, reference: {})", taskId, task.getTaskType(), task.getReferenceId());

        var timerSample = metricsConfig.startTaskExecutionTimer();
        var startTime = Instant.now();
        var executionLog = createExecutionLog(task, startTime);

        try {
            // Validate task can be executed
            if (!canExecute(task)) {
                log.warn("Task {} cannot be executed in current state: {}",
                        taskId, task.getStatus());
                return false;
            }

            // Get handler for this task type
            var handler = handlerRegistry.getHandlerOrThrow(task.getTaskType());

            // Validate task data
            try {
                handler.validate(task);
            } catch (IllegalArgumentException e) {
                log.error("Task {} validation failed: {}", taskId, e.getMessage());
                handlePermanentFailure(task, executionLog, "VALIDATION_ERROR", e.getMessage());
                return false;
            }

            // Execute the task
            var result = handler.execute(task);

            // Process result
            var endTime = Instant.now();
            var durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

            if (result.isSuccess()) {
                handleSuccess(task, executionLog, result, endTime, durationMs);
                metricsConfig.recordTaskExecution(timerSample, task.getTaskType(), true);
                return true;
            } else {
                handleFailure(task, executionLog, result, endTime, durationMs);
                metricsConfig.recordTaskExecution(timerSample, task.getTaskType(), false);
                metricsConfig.recordTaskFailure(task.getTaskType(), result.getErrorType());
                return false;
            }
        } catch (Exception e) {
            log.error("Unexpected error executing task {}: {}", taskId, e.getMessage(), e);
            var endTime = Instant.now();
            var durationMs = endTime.toEpochMilli() - startTime.toEpochMilli();

            var result = TaskExecutionResult.failure(e);
            handleFailure(task, executionLog, result, endTime, durationMs);

            metricsConfig.recordTaskExecution(timerSample, task.getTaskType(), false);
            metricsConfig.recordTaskFailure(task.getTaskType(), e.getClass().getSimpleName());

            return false;
        }
    }

    /**
     * Acquire lock on a task before execution
     */
    @Transactional
    public boolean acquireLock(ScheduledTask task) {
        var now = Instant.now();
        var lockUntil = now.plusSeconds(properties.getLockDurationMinutes() * 60L);

        var updated = taskRepository.acquireTaskLock(task.getId(), getInstanceId(), lockUntil, task.getVersion(), now);

        if (updated == 1) {
            log.debug("Acquired lock for task {} until {}", task.getId(), lockUntil);
            return true;
        } else {
            log.debug("Failed to acquire lock for task {} (already locked or version mismatch)", task.getId());
            return false;
        }
    }

    private boolean canExecute(ScheduledTask task) {
        // Check if task is expired
        if (task.isExpired()) {
            log.info("Task {} has expired, marking as EXPIRED", task.getId());
            task.setStatus(TaskStatus.EXPIRED);
            taskRepository.save(task);
            return false;
        }

        // Check if status allows execution
        return task.getStatus().isExecutable() || task.getStatus() == TaskStatus.PROCESSING;
    }

    private void handleSuccess(ScheduledTask task, TaskExecutionLog executionLog, TaskExecutionResult result, Instant endTime, long durationMs) {
        log.info("Task {} completed successfully in {}ms", task.getId(), durationMs);

        // Update task
        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(endTime);
        task.setExecutionDurationMs(durationMs);
        task.setLastError(null);
        task.setLastErrorStackTrace(null);
        task.setExecutionResult(result.getResponseData());
        task.setLockedBy(null);
        task.setLockedUntil(null);

        // Check for recurring task
        if (task.getCronExpression() != null && !task.getCronExpression().isBlank()) {
            // Calculate next execution time from cron expression
            // For now, recurring tasks need external rescheduling
            log.info("Task {} is recurring, would need rescheduling", task.getId());
        }

        taskRepository.save(task);

        // Update execution log
        executionLog.setStatus(TaskStatus.COMPLETED);
        executionLog.setCompletedAt(endTime);
        executionLog.setDurationMs(durationMs);
        executionLog.setSuccess(true);
        executionLog.setResponsePayload(result.getResponseData());
        executionLog.setHttpStatusCode(result.getHttpStatusCode());
        executionLogRepository.save(executionLog);
    }

    private void handleFailure(ScheduledTask task, TaskExecutionLog executionLog, TaskExecutionResult result, Instant endTime, long durationMs) {
        log.warn("Task {} failed: {}", task.getId(), result.getErrorMessage());

        // Update execution log first
        executionLog.setStatus(TaskStatus.FAILED);
        executionLog.setCompletedAt(endTime);
        executionLog.setDurationMs(durationMs);
        executionLog.setSuccess(false);
        executionLog.setErrorMessage(result.getErrorMessage());
        executionLog.setErrorStackTrace(result.getStackTrace());
        executionLog.setErrorType(result.getErrorType());
        executionLog.setHttpStatusCode(result.getHttpStatusCode());
        executionLog.setResponsePayload(result.getResponseData());
        executionLogRepository.save(executionLog);

        // Check if retryable
        if (!result.isRetryable()) {
            handlePermanentFailure(task, executionLog, result.getErrorType(), result.getErrorMessage());
            return;
        }

        // Check retry count
        var newRetryCount = task.getRetryCount() + 1;
        var maxRetries = task.getEffectiveMaxRetries(properties.getDefaultMaxRetries());

        if (newRetryCount >= maxRetries) {
            handleMaxRetriesExceeded(task, result);
            return;
        }

        // Schedule retry
        scheduleRetry(task, result, newRetryCount);
    }

    private void handlePermanentFailure(ScheduledTask task, TaskExecutionLog executionLog, String errorType, String errorMessage) {
        log.error("Task {} failed permanently (non-retryable): {}", task.getId(), errorMessage);

        task.setStatus(TaskStatus.DEAD_LETTER);
        task.setCompletedAt(Instant.now());
        task.setLastError(errorMessage);
        task.setLockedBy(null);
        task.setLockedUntil(null);

        taskRepository.save(task);

        // Send alert for permanent failures on critical tasks
        slackAlertService.sendTaskFailureAlert(task, errorMessage);
    }

    private void handleMaxRetriesExceeded(ScheduledTask task, TaskExecutionResult result) {
        log.error("Task {} exceeded max retries ({})", task.getId(), task.getRetryCount());

        task.setStatus(TaskStatus.MAX_RETRIES_EXCEEDED);
        task.setCompletedAt(Instant.now());
        task.setLastError(result.getErrorMessage());
        task.setLastErrorStackTrace(result.getStackTrace());
        task.setLockedBy(null);
        task.setLockedUntil(null);

        taskRepository.save(task);

        // Record metric
        metricsConfig.recordMaxRetriesExceeded(task.getTaskType());

        // Send Slack alert
        slackAlertService.sendMaxRetriesExceededAlert(task);
    }

    private void scheduleRetry(ScheduledTask task, TaskExecutionResult result, int newRetryCount) {
        // Calculate next retry time
        long delayMs;
        if (result.getCustomRetryDelayMs() != null) {
            delayMs = result.getCustomRetryDelayMs();
        } else {
            var handler = handlerRegistry.getHandlerOrThrow(task.getTaskType());
            delayMs = handler.calculateNextRetryDelayMs(task, properties.getDefaultRetryDelayHours());
        }

        var nextRetryTime = Instant.now().plusMillis(delayMs);

        log.info("Scheduling retry {} for task {} at {}", newRetryCount, task.getId(), nextRetryTime);

        task.setStatus(TaskStatus.RETRY_PENDING);
        task.setRetryCount(newRetryCount);
        task.setScheduledTime(nextRetryTime);
        task.setLastError(result.getErrorMessage());
        task.setLastErrorStackTrace(result.getStackTrace());
        task.setLockedBy(null);
        task.setLockedUntil(null);

        taskRepository.save(task);

        // Record retry metric
        metricsConfig.recordRetry(task.getTaskType(), newRetryCount);
    }

    private TaskExecutionLog createExecutionLog(ScheduledTask task, Instant startTime) {
        var log = TaskExecutionLog.builder()
                .taskId(task.getId())
                .attemptNumber(task.getRetryCount() + 1)
                .status(TaskStatus.PROCESSING)
                .executorInstance(getInstanceId())
                .startedAt(startTime)
                .success(false)
                .requestPayload(buildRequestPayload(task))
                .build();

        return executionLogRepository.save(log);
    }

    private Map<String, Object> buildRequestPayload(ScheduledTask task) {
        var payload = new HashMap<String, Object>();
        payload.put("taskId", task.getId().toString());
        payload.put("taskType", task.getTaskType().name());
        payload.put("referenceId", task.getReferenceId());
        payload.put("secondaryReferenceId", task.getSecondaryReferenceId());
        payload.put("attemptNumber", task.getRetryCount() + 1);
        if (task.getPayload() != null) {
            payload.put("taskPayload", task.getPayload());
        }
        return payload;
    }
}
