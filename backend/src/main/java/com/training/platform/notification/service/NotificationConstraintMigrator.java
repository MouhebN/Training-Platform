package com.training.platform.notification.service;

import com.training.platform.notification.entity.NotificationType;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class NotificationConstraintMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public NotificationConstraintMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String allowed = Arrays.stream(NotificationType.values())
                .map(type -> "'" + type.name() + "'")
                .collect(Collectors.joining(", "));
        jdbcTemplate.execute("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
        jdbcTemplate.execute(
                "ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (" + allowed + "))"
        );
    }
}
