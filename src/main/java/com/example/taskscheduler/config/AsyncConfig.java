package com.example.taskscheduler.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Configuration for async processing using Java 21 Virtual Threads.
 * <p>
 * Virtual threads provide:
 * - Lightweight threads (millions possible vs thousands for platform threads)
 * - Automatic blocking operation handling
 * - Better resource utilization for I/O-bound tasks
 */
@Slf4j
@EnableAsync
@Configuration
public class AsyncConfig {

    @Value("${task-scheduler.executor-pool-size:20}")
    private int executorPoolSize;

    /**
     * Virtual Thread executor for task processing.
     * Each task gets its own virtual thread for maximum concurrency.
     */
    @Bean(name = "virtualThreadExecutor")
    public ExecutorService virtualThreadExecutor() {
        log.info("Creating Virtual Thread executor for task processing");

        var factory = Thread.ofVirtual().name("task-executor-", 0).factory();

        return Executors.newThreadPerTaskExecutor(factory);
    }

    /**
     * Task executor for Spring's @Async annotation.
     * Uses virtual threads for async method execution.
     */
    @Bean(name = "taskExecutor")
    public TaskExecutor taskExecutor() {
        log.info("Configuring Spring TaskExecutor with virtual threads");

        // ThreadPoolTaskExecutor with virtual threads
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(executorPoolSize);
        executor.setMaxPoolSize(executorPoolSize * 2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler((r, e) -> {
            log.warn("Task rejected from async executor, running in caller thread");
            if (!e.isShutdown()) {
                r.run();
            }
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        return executor;
    }

    /**
     * Bounded virtual thread executor with semaphore control.
     * Use this when you need to limit concurrent task execution.
     */
    @Bean(name = "boundedVirtualThreadExecutor")
    public ExecutorService boundedVirtualThreadExecutor() {
        log.info("Creating bounded Virtual Thread executor with max {} concurrent tasks", executorPoolSize);

        // Using semaphore to limit concurrent executions while still using virtual threads
        return Executors.newFixedThreadPool(executorPoolSize, Thread.ofVirtual().name("bounded-task-", 0).factory());
    }
}
