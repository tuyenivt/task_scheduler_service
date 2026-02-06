package com.example.taskscheduler.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "external-services.payment-service")
public class PaymentServiceProperties {
    @NotBlank
    private String baseUrl;
    private int timeoutSeconds = 30;
}
