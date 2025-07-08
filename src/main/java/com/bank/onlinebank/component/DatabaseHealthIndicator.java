package com.bank.onlinebank.component;

import com.bank.onlinebank.service.DatabaseHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Кастомный Health Indicator для интеграции с Spring Boot Actuator.
 * <p>
 * Предоставляет детальную информацию о статусе подключения к БД
 * в эндпоинте /api/health.
 * </p>
 */
@Component("database") // Называем его "database" для красивого вывода в JSON
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DatabaseHealthService healthService;

    @Autowired
    public DatabaseHealthIndicator(DatabaseHealthService healthService) {
        this.healthService = healthService;
    }

    @Override
    public Health health() {
        if (healthService.isDatabaseConnected()) {
            return Health.up()
                    .withDetail("version", healthService.getDatabaseVersion())
                    .withDetail("message", "Подключение к базе данных установлено")
                    .build();
        } else {
            return Health.down()
                    .withDetail("error", "База данных недоступна")
                    .withDetail("message", "Не удалось установить подключение к базе данных.")
                    .build();
        }
    }
}