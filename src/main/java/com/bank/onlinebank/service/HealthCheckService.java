package com.bank.onlinebank.service;

import com.bank.onlinebank.config.DatabaseConfig;
import com.bank.onlinebank.dto.HealthCheckDTO;
import com.bank.onlinebank.dto.MetricsDTO;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Сервис для мониторинга состояния приложения и его компонентов
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckService {

    private final DatabaseConfig.DatabaseHealthIndicator databaseHealthIndicator;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.application.name:OnlineBank}")
    private String applicationName;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    // Метрики API
    private final Map<String, AtomicLong> apiCallCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> apiResponseTimes = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> apiResponseTimeHistory = new ConcurrentHashMap<>();

    private final LocalDateTime startupTime = LocalDateTime.now();

    /**
     * Получить полную информацию о состоянии системы
     */
    public HealthCheckDTO getHealthStatus() {
        log.debug("Получение статуса здоровья системы");

        HealthCheckDTO health = new HealthCheckDTO();
        health.setTimestamp(LocalDateTime.now());
        health.setStatus(calculateOverallStatus());
        health.setApplication(getApplicationInfo());
        health.setJvm(getJvmInfo());
        health.setDatabase(getDatabaseInfo());
        health.setMetrics(getMetricsInfo());
        health.setUptime(calculateUptime());

        return health;
    }

    /**
     * Записать метрику вызова API
     */
    public void recordApiCall(String endpoint, long responseTime) {
        // Увеличиваем счетчик вызовов
        apiCallCounts.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();

        // Добавляем время ответа
        apiResponseTimes.computeIfAbsent(endpoint, k -> new AtomicLong(0)).addAndGet(responseTime);

        // Сохраняем историю последних 100 вызовов для каждого endpoint
        apiResponseTimeHistory.computeIfAbsent(endpoint, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(responseTime);

        // Ограничиваем размер истории
        List<Long> history = apiResponseTimeHistory.get(endpoint);
        if (history.size() > 100) {
            history.remove(0);
        }
    }

    /**
     * Определить общий статус системы
     */
    private String calculateOverallStatus() {
        boolean dbHealthy = databaseHealthIndicator.isHealthy();
        boolean jvmHealthy = isJvmHealthy();

        if (dbHealthy && jvmHealthy) {
            return "UP";
        } else if (!dbHealthy && jvmHealthy) {
            return "DEGRADED";
        } else {
            return "DOWN";
        }
    }

    /**
     * Получить информацию о приложении
     */
    private Map<String, Object> getApplicationInfo() {
        Map<String, Object> appInfo = new HashMap<>();
        appInfo.put("name", applicationName);
        appInfo.put("profile", activeProfile);
        appInfo.put("startupTime", startupTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        appInfo.put("version", getClass().getPackage().getImplementationVersion() != null ?
                getClass().getPackage().getImplementationVersion() : "0.0.1-SNAPSHOT");

        return appInfo;
    }

    /**
     * Получить информацию о JVM
     */
    private Map<String, Object> getJvmInfo() {
        Map<String, Object> jvmInfo = new HashMap<>();

        // Runtime информация
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        jvmInfo.put("version", System.getProperty("java.version"));
        jvmInfo.put("vendor", System.getProperty("java.vendor"));
        jvmInfo.put("vmName", runtimeBean.getVmName());
        jvmInfo.put("vmVersion", runtimeBean.getVmVersion());

        // Memory информация
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

        Map<String, Object> memory = new HashMap<>();
        memory.put("heapUsed", formatBytes(heapUsed));
        memory.put("heapMax", formatBytes(heapMax));
        memory.put("heapUsedPercentage", Math.round((double) heapUsed / heapMax * 100));
        memory.put("nonHeapUsed", formatBytes(nonHeapUsed));
        jvmInfo.put("memory", memory);

        // CPU информация
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> cpu = new HashMap<>();
        cpu.put("availableProcessors", osBean.getAvailableProcessors());
        cpu.put("systemLoadAverage", osBean.getSystemLoadAverage());
        cpu.put("processCpuLoad", getProcessCpuLoad());
        jvmInfo.put("cpu", cpu);

        // OS информация
        Map<String, Object> os = new HashMap<>();
        os.put("name", osBean.getName());
        os.put("version", osBean.getVersion());
        os.put("arch", osBean.getArch());
        jvmInfo.put("os", os);

        return jvmInfo;
    }

    /**
     * Получить информацию о базе данных
     */
    private Map<String, Object> getDatabaseInfo() {
        Map<String, Object> dbInfo = new HashMap<>();

        boolean isHealthy = databaseHealthIndicator.isHealthy();
        dbInfo.put("status", isHealthy ? "UP" : "DOWN");

        if (isHealthy) {
            try {
                // Версия PostgreSQL
                String version = jdbcTemplate.queryForObject(
                        "SELECT version()", String.class);
                dbInfo.put("version", version);

                // Текущая схема
                String schema = jdbcTemplate.queryForObject(
                        "SELECT current_schema()", String.class);
                dbInfo.put("schema", schema);

                // Количество клиентов
                Long clientCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM clients", Long.class);
                dbInfo.put("clientCount", clientCount);

                // Информация о пуле соединений
                if (dataSource instanceof HikariDataSource) {
                    HikariDataSource hikariDS = (HikariDataSource) dataSource;
                    HikariPoolMXBean poolMXBean = hikariDS.getHikariPoolMXBean();

                    Map<String, Object> poolInfo = new HashMap<>();
                    poolInfo.put("activeConnections", poolMXBean.getActiveConnections());
                    poolInfo.put("idleConnections", poolMXBean.getIdleConnections());
                    poolInfo.put("totalConnections", poolMXBean.getTotalConnections());
                    poolInfo.put("threadsAwaitingConnection", poolMXBean.getThreadsAwaitingConnection());

                    dbInfo.put("connectionPool", poolInfo);
                }

            } catch (Exception e) {
                log.error("Ошибка при получении информации о БД: {}", e.getMessage());
                dbInfo.put("error", e.getMessage());
            }
        } else {
            dbInfo.put("error", "База данных недоступна");
        }

        return dbInfo;
    }

    /**
     * Получить метрики производительности API
     */
    private MetricsDTO getMetricsInfo() {
        MetricsDTO metrics = new MetricsDTO();

        // Общие метрики
        long totalCalls = apiCallCounts.values().stream()
                .mapToLong(AtomicLong::get).sum();
        long totalResponseTime = apiResponseTimes.values().stream()
                .mapToLong(AtomicLong::get).sum();

        metrics.setTotalApiCalls(totalCalls);
        metrics.setAverageResponseTime(totalCalls > 0 ? totalResponseTime / totalCalls : 0);

        // Метрики по endpoint'ам
        Map<String, Map<String, Object>> endpointMetrics = new HashMap<>();

        apiCallCounts.forEach((endpoint, count) -> {
            Map<String, Object> endpointData = new HashMap<>();
            long calls = count.get();
            long totalTime = apiResponseTimes.getOrDefault(endpoint, new AtomicLong(0)).get();

            endpointData.put("calls", calls);
            endpointData.put("averageTime", calls > 0 ? totalTime / calls : 0);

            // Рассчитываем перцентили
            List<Long> history = apiResponseTimeHistory.get(endpoint);
            if (history != null && !history.isEmpty()) {
                List<Long> sorted = new ArrayList<>(history);
                Collections.sort(sorted);

                endpointData.put("p50", getPercentile(sorted, 50));
                endpointData.put("p95", getPercentile(sorted, 95));
                endpointData.put("p99", getPercentile(sorted, 99));
            }

            endpointMetrics.put(endpoint, endpointData);
        });

        metrics.setEndpointMetrics(endpointMetrics);

        return metrics;
    }

    /**
     * Рассчитать время работы приложения
     */
    private String calculateUptime() {
        Duration uptime = Duration.between(startupTime, LocalDateTime.now());
        long days = uptime.toDays();
        long hours = uptime.toHours() % 24;
        long minutes = uptime.toMinutes() % 60;

        return String.format("%d дней, %d часов, %d минут", days, hours, minutes);
    }

    /**
     * Проверить здоровье JVM
     */
    private boolean isJvmHealthy() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();

        // Считаем нездоровым, если используется более 90% heap
        double heapUsagePercent = (double) heapUsed / heapMax * 100;
        return heapUsagePercent < 90;
    }

    /**
     * Получить загрузку CPU процесса
     */
    private double getProcessCpuLoad() {
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
            }
        } catch (Exception e) {
            log.debug("Не удалось получить загрузку CPU: {}", e.getMessage());
        }
        return -1;
    }

    /**
     * Форматировать байты в читаемый вид
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Вычислить перцентиль
     */
    private long getPercentile(List<Long> sorted, int percentile) {
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, index));
    }
}