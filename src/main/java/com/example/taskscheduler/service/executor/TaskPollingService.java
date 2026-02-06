package com.example.taskscheduler.service.executor;

import com.example.taskscheduler.config.TaskSchedulerProperties;
import com.example.taskscheduler.domain.entity.ScheduledTask;
import com.example.taskscheduler.domain.repository.ScheduledTaskRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service responsible for polling pending tasks and dispatching them for execution.
 * <p>
 * Uses ShedLock to ensure only one instance polls at a time across the EKS cluster.
 * Individual task locking uses PostgreSQL's SKIP LOCKED for efficient distribution.
 * <p>
 * Flow:
 * 1. Poll job runs on schedule (e.g., every 30 seconds)
 * 2. Fetches batch of ready tasks using FOR UPDATE SKIP LOCKED
 * 3. Dispatches each task to the virtual thread executor
 * 4. Each task execution handles its own locking and updates
 */
@Slf4j
@Service
public class TaskPollingService {

    private final ScheduledTaskRepository taskRepository;
    private final TaskExecutorService taskExecutorService;
    private final TaskSchedulerProperties properties;
    private final ExecutorService virtualThreadExecutor;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public TaskPollingService(ScheduledTaskRepository taskRepository, TaskExecutorService taskExecutorService, TaskSchedulerProperties properties,
                              @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.taskRepository = taskRepository;
        this.taskExecutorService = taskExecutorService;
        this.properties = properties;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    /**
     * Main polling job - fetches and processes pending tasks.
     * <p>
     * ShedLock ensures this only runs on one instance at a time,
     * but the actual task processing is distributed across all instances
     * through the SKIP LOCKED mechanism.
     */
    @Scheduled(fixedDelayString = "${task-scheduler.poll-interval-ms:30000}")
    @SchedulerLock(name = "taskPollingJob", lockAtLeastFor = "10s", lockAtMostFor = "5m")
    public void pollAndProcessTasks() {
        if (!isRunning.compareAndSet(false, true)) {
            log.debug("Previous polling cycle still running, skipping");
            return;
        }

        try {
            log.debug("Starting task polling cycle");
            var now = Instant.now();

            // Fetch batch of ready tasks
            var tasks = taskRepository.findTasksForExecution(now, properties.getBatchSize());

            if (tasks.isEmpty()) {
                log.debug("No tasks ready for execution");
                return;
            }

            log.info("Found {} tasks ready for execution", tasks.size());

            // Dispatch tasks to virtual thread executor
            var futures = tasks.stream()
                    .map(task -> CompletableFuture.supplyAsync(
                            () -> processTask(task),
                            virtualThreadExecutor
                    )).toList();

            // Wait for all tasks to complete (with timeout)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(properties.getLockDurationMinutes(), java.util.concurrent.TimeUnit.MINUTES)
                    .exceptionally(ex -> {
                        log.error("Error waiting for task completion: {}", ex.getMessage());
                        return null;
                    })
                    .join();

            // Count successes
            var successCount = futures.stream()
                    .filter(f -> {
                        try {
                            return f.isDone() && !f.isCompletedExceptionally() && f.get();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();

            log.info("Completed processing {} tasks, {} successful", tasks.size(), successCount);
        } catch (Exception e) {
            log.error("Error in task polling cycle: {}", e.getMessage(), e);
        } finally {
            isRunning.set(false);
        }
    }

    /**
     * Process a single task - acquire lock and execute.
     */
    private boolean processTask(ScheduledTask task) {
        var taskId = task.getId();

        try {
            // Try to acquire lock
            if (!taskExecutorService.acquireLock(task)) {
                log.debug("Failed to acquire lock for task {}, skipping", taskId);
                return false;
            }

            // Execute task
            return taskExecutorService.executeTask(task);
        } catch (Exception e) {
            log.error("Error processing task {}: {}", taskId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cleanup stale tasks - runs periodically to handle orphaned locks.
     * <p>
     * Tasks can become stale if:
     * - Instance crashes while processing
     * - Task execution times out
     * - Network partitions occur
     */
    @Scheduled(fixedDelayString = "${task-scheduler.stale-task-check-interval-ms:300000}")
    @SchedulerLock(name = "staleTaskCleanup", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    public void cleanupStaleTasks() {
        try {
            var threshold = Instant.now().minusSeconds(properties.getStaleTaskThresholdMinutes() * 60L);

            var staleTasks = taskRepository.findStaleTasks(threshold);

            if (staleTasks.isEmpty()) {
                log.debug("No stale tasks found");
                return;
            }

            log.warn("Found {} stale tasks, resetting for retry", staleTasks.size());

            var taskIds = staleTasks.stream().map(ScheduledTask::getId).toList();

            // Schedule retry for next polling cycle
            var nextRetryTime = Instant.now().plusSeconds(60);

            var resetCount = taskRepository.resetStaleTasks(taskIds, nextRetryTime, Instant.now());
            log.info("Reset {} stale tasks for retry", resetCount);
        } catch (Exception e) {
            log.error("Error cleaning up stale tasks: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a specific task immediately (for manual triggers)
     */
    public CompletableFuture<Boolean> processTaskAsync(UUID taskId) {
        return CompletableFuture.supplyAsync(() -> {
            var task = taskRepository.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
            return processTask(task);
        }, virtualThreadExecutor);
    }
}
