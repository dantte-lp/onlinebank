package com.bank.onlinebank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO для метрик производительности
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricsDTO {

    private Long totalApiCalls;

    private Long averageResponseTime; // в миллисекундах

    private Map<String, Map<String, Object>> endpointMetrics;

    private Map<String, Object> actuatorMetrics;

    /**
     * Получить метрики для конкретного endpoint
     */
    public Map<String, Object> getEndpointMetric(String endpoint) {
        return endpointMetrics != null ? endpointMetrics.get(endpoint) : null;
    }
}