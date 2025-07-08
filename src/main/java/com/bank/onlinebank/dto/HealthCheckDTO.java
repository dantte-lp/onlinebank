
package com.bank.onlinebank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO для передачи информации о состоянии системы
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthCheckDTO {

    private LocalDateTime timestamp;

    private String status; // UP, DOWN, DEGRADED

    private Map<String, Object> application;

    private Map<String, Object> jvm;

    private Map<String, Object> database;

    private MetricsDTO metrics;

    private String uptime;

    /**
     * Проверка, работает ли система нормально
     */
    public boolean isHealthy() {
        return "UP".equals(status);
    }

    /**
     * Проверка, работает ли система в ограниченном режиме
     */
    public boolean isDegraded() {
        return "DEGRADED".equals(status);
    }
}