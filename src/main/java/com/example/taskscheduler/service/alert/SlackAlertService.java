package com.example.taskscheduler.service.alert;

import com.example.taskscheduler.config.SlackProperties;
import com.example.taskscheduler.domain.entity.ScheduledTask;
import com.example.taskscheduler.domain.enums.TaskPriority;
import com.slack.api.Slack;
import com.slack.api.model.Attachment;
import com.slack.api.model.Field;
import com.slack.api.webhook.Payload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Service for sending alerts to Slack when tasks reach max retries.
 * <p>
 * Sends formatted messages to the on-call channel with task details
 * to enable quick investigation and resolution.
 */
@Slf4j
@Service
public class SlackAlertService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private final SlackProperties slackProperties;
    private final Slack slack = Slack.getInstance();

    @Value("${spring.application.name:task-scheduler}")
    private String applicationName;

    public SlackAlertService(SlackProperties slackProperties) {
        this.slackProperties = slackProperties;
    }

    /**
     * Send alert for max retries exceeded.
     * Runs asynchronously to not block task processing.
     */
    @Async
    public void sendMaxRetriesExceededAlert(ScheduledTask task) {
        if (!slackProperties.isEnabled() || slackProperties.getWebhookUrl() == null || slackProperties.getWebhookUrl().isBlank()) {
            log.warn("Slack alerting is disabled or webhook URL not configured. Task {} reached max retries but no alert was sent.", task.getId());
            return;
        }

        try {
            var payload = buildMaxRetriesPayload(task);
            var response = slack.send(slackProperties.getWebhookUrl(), payload);

            if (response.getCode() != 200) {
                log.error("Failed to send Slack alert. Response code: {}, body: {}", response.getCode(), response.getBody());
            } else {
                log.info("Slack alert sent for task {} max retries exceeded", task.getId());
            }
        } catch (Exception e) {
            log.error("Error sending Slack alert for task {}: {}", task.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send generic error alert
     */
    @Async
    public void sendErrorAlert(String title, String message, String details) {
        if (!slackProperties.isEnabled() || slackProperties.getWebhookUrl() == null || slackProperties.getWebhookUrl().isBlank()) {
            log.warn("Slack alerting disabled. Error alert not sent: {}", title);
            return;
        }

        try {
            var payload = Payload.builder()
                    .channel(slackProperties.getChannel())
                    .username(applicationName)
                    .iconEmoji(":warning:")
                    .text(":warning: *" + title + "*")
                    .attachments(List.of(
                            Attachment.builder()
                                    .color("warning")
                                    .text(message)
                                    .fields(details != null ? List.of(
                                            Field.builder()
                                                    .title("Details")
                                                    .value(truncate(details, 500))
                                                    .valueShortEnough(false)
                                                    .build()
                                    ) : List.of())
                                    .footer(applicationName)
                                    .ts(String.valueOf(Instant.now().getEpochSecond()))
                                    .build()
                    ))
                    .build();

            slack.send(slackProperties.getWebhookUrl(), payload);
        } catch (Exception e) {
            log.error("Error sending Slack error alert: {}", e.getMessage(), e);
        }
    }

    /**
     * Send task failure alert (for critical tasks)
     */
    @Async
    public void sendTaskFailureAlert(ScheduledTask task, String errorMessage) {
        if (!slackProperties.isEnabled() || slackProperties.getWebhookUrl() == null || slackProperties.getWebhookUrl().isBlank()) {
            return;
        }

        // Only send for critical priority tasks
        if (task.getPriority() == null || task.getPriority().getValue() < TaskPriority.HIGH.getValue()) {
            return;
        }

        try {
            var payload = Payload.builder()
                    .channel(slackProperties.getChannel())
                    .username(applicationName)
                    .iconEmoji(":rotating_light:")
                    .text(":rotating_light: *Critical Task Failed*")
                    .attachments(List.of(
                            Attachment.builder()
                                    .color("danger")
                                    .title("Task: " + task.getTaskType().getDisplayName())
                                    .titleLink(buildTaskLink(task.getId().toString()))
                                    .fields(Arrays.asList(
                                            Field.builder()
                                                    .title("Task ID")
                                                    .value(task.getId().toString())
                                                    .valueShortEnough(true)
                                                    .build(),
                                            Field.builder()
                                                    .title("Reference")
                                                    .value(task.getReferenceId())
                                                    .valueShortEnough(true)
                                                    .build(),
                                            Field.builder()
                                                    .title("Error")
                                                    .value(truncate(errorMessage, 300))
                                                    .valueShortEnough(false)
                                                    .build()
                                    ))
                                    .footer(applicationName)
                                    .ts(String.valueOf(Instant.now().getEpochSecond()))
                                    .build()
                    ))
                    .build();

            slack.send(slackProperties.getWebhookUrl(), payload);
        } catch (Exception e) {
            log.error("Error sending Slack task failure alert: {}", e.getMessage(), e);
        }
    }

    private Payload buildMaxRetriesPayload(ScheduledTask task) {
        var taskId = task.getId().toString();
        var taskType = task.getTaskType().getDisplayName();
        var referenceId = task.getReferenceId();
        var lastError = task.getLastError() != null ? task.getLastError() : "Unknown error";

        return Payload.builder()
                .channel(slackProperties.getChannel())
                .username(applicationName)
                .iconEmoji(":rotating_light:")
                .text(":rotating_light: *Task Max Retries Exceeded - Manual Intervention Required*")
                .attachments(List.of(
                        Attachment.builder()
                                .color("danger")
                                .title(taskType + " - " + referenceId)
                                .titleLink(buildTaskLink(taskId))
                                .fields(Arrays.asList(
                                        Field.builder()
                                                .title("Task ID")
                                                .value(taskId)
                                                .valueShortEnough(true)
                                                .build(),
                                        Field.builder()
                                                .title("Task Type")
                                                .value(taskType)
                                                .valueShortEnough(true)
                                                .build(),
                                        Field.builder()
                                                .title("Reference ID")
                                                .value(referenceId)
                                                .valueShortEnough(true)
                                                .build(),
                                        Field.builder()
                                                .title("Retry Count")
                                                .value(String.valueOf(task.getRetryCount()))
                                                .valueShortEnough(true)
                                                .build(),
                                        Field.builder()
                                                .title("Created At")
                                                .value(DATE_FORMATTER.format(task.getCreatedAt()))
                                                .valueShortEnough(true)
                                                .build(),
                                        Field.builder()
                                                .title("Last Error")
                                                .value("```" + truncate(lastError, 400) + "```")
                                                .valueShortEnough(false)
                                                .build()
                                ))
                                .footer(applicationName + " | Please investigate and manually retry or cancel")
                                .ts(String.valueOf(Instant.now().getEpochSecond()))
                                .build()
                ))
                .build();
    }

    private String buildTaskLink(String taskId) {
        return slackProperties.getDashboardBaseUrl() + "/tasks/" + taskId;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
