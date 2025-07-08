package com.bank.onlinebank.controller;

import com.bank.onlinebank.dto.HealthCheckDTO;
import com.bank.onlinebank.service.HealthCheckService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST контроллер для проверки состояния системы
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class HealthCheckController {

    private final HealthCheckService healthCheckService;

    /**
     * Простая проверка доступности (для load balancer)
     */
    @GetMapping("/ping")
    @Timed(value = "health.ping", description = "Health ping endpoint")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    /**
     * Базовая проверка здоровья
     */
    @GetMapping
    @Timed(value = "health.check", description = "Basic health check")
    public ResponseEntity<HealthStatus> health() {
        HealthCheckDTO health = healthCheckService.getHealthStatus();

        HealthStatus status = new HealthStatus(
                health.getStatus(),
                health.getTimestamp().toString()
        );

        HttpStatus httpStatus = switch (health.getStatus()) {
            case "UP" -> HttpStatus.OK;
            case "DEGRADED" -> HttpStatus.OK; // Возвращаем 200, но с пометкой DEGRADED
            default -> HttpStatus.SERVICE_UNAVAILABLE;
        };

        return ResponseEntity.status(httpStatus).body(status);
    }

    /**
     * Полная проверка здоровья с детальной информацией
     */
    @GetMapping("/detailed")
    @Timed(value = "health.detailed", description = "Detailed health check")
    public ResponseEntity<HealthCheckDTO> detailedHealth() {
        log.debug("Запрос детальной информации о состоянии системы");

        HealthCheckDTO health = healthCheckService.getHealthStatus();

        HttpStatus httpStatus = health.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus)
                .contentType(MediaType.APPLICATION_JSON)
                .body(health);
    }

    /**
     * Проверка состояния базы данных
     */
    @GetMapping("/database")
    @Timed(value = "health.database", description = "Database health check")
    public ResponseEntity<DatabaseStatus> databaseHealth() {
        HealthCheckDTO health = healthCheckService.getHealthStatus();

        @SuppressWarnings("unchecked")
        var dbInfo = (java.util.Map<String, Object>) health.getDatabase();

        DatabaseStatus dbStatus = new DatabaseStatus(
                (String) dbInfo.get("status"),
                (String) dbInfo.get("version"),
                (String) dbInfo.get("schema"),
                dbInfo.get("connectionPool")
        );

        HttpStatus httpStatus = "UP".equals(dbStatus.status()) ?
                HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(httpStatus).body(dbStatus);
    }

    /**
     * Метрики производительности API
     */
    @GetMapping("/metrics")
    @Timed(value = "health.metrics", description = "API metrics")
    public ResponseEntity<MetricsResponse> metrics() {
        HealthCheckDTO health = healthCheckService.getHealthStatus();

        MetricsResponse response = new MetricsResponse(
                health.getMetrics().getTotalApiCalls(),
                health.getMetrics().getAverageResponseTime(),
                health.getMetrics().getEndpointMetrics(),
                health.getUptime()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Проверка готовности (для Kubernetes readiness probe)
     */
    @GetMapping("/ready")
    public ResponseEntity<Void> ready() {
        HealthCheckDTO health = healthCheckService.getHealthStatus();

        // Приложение готово, если БД доступна или работает в деградированном режиме
        if (health.isHealthy() || health.isDegraded()) {
            return ResponseEntity.ok().build();
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Проверка жизнеспособности (для Kubernetes liveness probe)
     */
    @GetMapping("/live")
    public ResponseEntity<Void> live() {
        // Если endpoint отвечает, значит приложение живо
        return ResponseEntity.ok().build();
    }

    // Response DTOs

    record HealthStatus(String status, String timestamp) {}

    record DatabaseStatus(
            String status,
            String version,
            String schema,
            Object connectionPool
    ) {}

    record MetricsResponse(
            Long totalApiCalls,
            Long averageResponseTime,
            java.util.Map<String, java.util.Map<String, Object>> endpointMetrics,
            String uptime
    ) {}
}