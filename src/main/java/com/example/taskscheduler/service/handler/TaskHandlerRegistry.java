package com.example.taskscheduler.service.handler;

import com.example.taskscheduler.domain.enums.TaskType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Registry for task handlers.
 * <p>
 * Automatically discovers and registers all TaskHandler beans.
 * Provides lookup by task type.
 */
@Slf4j
@Component
public class TaskHandlerRegistry {

    private final Map<TaskType, TaskHandler> handlers = new EnumMap<>(TaskType.class);
    private final List<TaskHandler> handlerBeans;

    public TaskHandlerRegistry(List<TaskHandler> handlerBeans) {
        this.handlerBeans = handlerBeans;
    }

    @PostConstruct
    public void initialize() {
        for (var handler : handlerBeans) {
            var type = handler.getTaskType();
            if (handlers.containsKey(type)) {
                log.warn("Duplicate handler for task type {}: {} will override {}",
                        type, handler.getClass().getSimpleName(),
                        handlers.get(type).getClass().getSimpleName());
            }
            handlers.put(type, handler);
            log.info("Registered handler for task type {}: {}", type, handler.getClass().getSimpleName());
        }

        // Log warning for unregistered task types
        for (var type : TaskType.values()) {
            if (!handlers.containsKey(type)) {
                log.warn("No handler registered for task type: {}", type);
            }
        }
    }

    /**
     * Get handler for a task type
     *
     * @param taskType The task type
     * @return Optional containing the handler if found
     */
    public Optional<TaskHandler> getHandler(TaskType taskType) {
        return Optional.ofNullable(handlers.get(taskType));
    }

    /**
     * Get handler for a task type, throwing if not found
     *
     * @param taskType The task type
     * @return The handler
     * @throws IllegalArgumentException if no handler is registered
     */
    public TaskHandler getHandlerOrThrow(TaskType taskType) {
        return getHandler(taskType).orElseThrow(() -> new IllegalArgumentException("No handler registered for task type: " + taskType));
    }

    /**
     * Check if a handler exists for a task type
     */
    public boolean hasHandler(TaskType taskType) {
        return handlers.containsKey(taskType);
    }

    /**
     * Get all registered task types
     */
    public Set<TaskType> getRegisteredTypes() {
        return handlers.keySet();
    }

    /**
     * Get count of registered handlers
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
