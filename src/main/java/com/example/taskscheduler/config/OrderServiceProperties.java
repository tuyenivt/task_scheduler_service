package com.example.taskscheduler.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * External service configuration properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "external-services.order-service")
public class OrderServiceProperties {
    @NotBlank
    private String baseUrl;
    private int timeoutSeconds = 30;
}
