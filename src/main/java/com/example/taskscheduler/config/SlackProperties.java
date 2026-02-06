package com.example.taskscheduler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Slack configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "slack")
public class SlackProperties {
    private String webhookUrl;
    private String channel = "#oncall-alerts";
    private boolean enabled = true;
}
