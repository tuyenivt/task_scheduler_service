package com.example.taskscheduler.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ShedLock configuration for distributed scheduler locking.
 * <p>
 * Ensures that scheduled tasks (like the polling job) run on only one
 * instance at a time in a multi-instance EKS deployment.
 * <p>
 * The actual task-level locking is handled by PostgreSQL's
 * FOR UPDATE SKIP LOCKED in the repository.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
public class ShedLockConfig {

    /**
     * JDBC-based lock provider using PostgreSQL.
     * Stores lock information in the shedlock table.
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(
                JdbcTemplateLockProvider.Configuration.builder()
                        .withJdbcTemplate(new JdbcTemplate(dataSource))
                        .withTableName("shedlock")
                        .usingDbTime()
                        .build()
        );
    }
}
