package com.bank.onlinebank.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Конфигурация базы данных с поддержкой работы без подключения к БД
 */
@Slf4j
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.bank.onlinebank.repository")
@EnableJpaAuditing
public class DatabaseConfig {

    @Autowired
    private Environment env;

    private final AtomicBoolean isDatabaseAvailable = new AtomicBoolean(false);
    private final AtomicBoolean isSchemaInitialized = new AtomicBoolean(false);
    private HikariDataSource realDataSource;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = env.getProperty("spring.datasource.url", "jdbc:postgresql://localhost:5432/onlinebank");
        String dbUsername = env.getProperty("spring.datasource.username", "postgres");
        String dbPassword = env.getProperty("spring.datasource.password", "postgres");

        // Создаем реальный DataSource
        realDataSource = new HikariDataSource();
        realDataSource.setJdbcUrl(dbUrl);
        realDataSource.setUsername(dbUsername);
        realDataSource.setPassword(dbPassword);
        realDataSource.setDriverClassName("org.postgresql.Driver");

        // Важные настройки для resilience
        realDataSource.setMaximumPoolSize(10);
        realDataSource.setMinimumIdle(0); // Позволяет пулу быть пустым
        realDataSource.setConnectionTimeout(5000); // 5 секунд на попытку
        realDataSource.setValidationTimeout(3000);
        realDataSource.setInitializationFailTimeout(0); // Не блокировать при старте
        realDataSource.setPoolName("OnlineBankHikariPool");

        // Настройки для автоматического восстановления
        realDataSource.setLeakDetectionThreshold(60000);
        realDataSource.setIdleTimeout(600000);
        realDataSource.setMaxLifetime(1800000);

        // Проверяем доступность БД
        checkDatabaseConnection();

        // Запускаем периодическую проверку БД каждые 10 секунд
        scheduler.scheduleWithFixedDelay(this::checkDatabaseConnection, 10, 10, TimeUnit.SECONDS);

        // Возвращаем прокси, который будет управлять доступом к БД
        return new ResilientDataSource(realDataSource, this);
    }

    /**
     * Проверяет подключение к БД и инициализирует схему при необходимости
     */
    private void checkDatabaseConnection() {
        try (Connection conn = realDataSource.getConnection()) {
            conn.isValid(2);

            boolean wasUnavailable = !isDatabaseAvailable.getAndSet(true);

            if (wasUnavailable) {
                log.info("База данных стала доступна!");

                // Инициализируем схему при первом подключении
                if (!isSchemaInitialized.getAndSet(true)) {
                    initializeSchema();
                }
            }

        } catch (SQLException e) {
            boolean wasAvailable = isDatabaseAvailable.getAndSet(false);
            if (wasAvailable) {
                log.warn("База данных стала недоступна: {}", e.getMessage());
            }
        }
    }

    /**
     * Инициализирует схему БД
     */
    private void initializeSchema() {
        log.info("Инициализация схемы базы данных...");

        try {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(realDataSource);

            // Проверяем существование таблицы clients
            try {
                jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clients", Integer.class);
                log.info("Схема уже существует");
            } catch (Exception e) {
                // Таблица не существует, создаем схему
                log.info("Создание схемы БД...");

                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(new ClassPathResource("schema.sql"));
                populator.setSeparator(";");
                populator.execute(realDataSource);

                log.info("Схема БД успешно создана");
            }

        } catch (Exception e) {
            log.error("Ошибка при инициализации схемы: {}", e.getMessage());
            isSchemaInitialized.set(false); // Попробуем еще раз позже
        }
    }

    /**
     * Bean для проверки статуса БД
     */
    @Bean
    public DatabaseHealthIndicator databaseHealthIndicator() {
        return new DatabaseHealthIndicator(this);
    }

    /**
     * Проверяет доступность БД
     */
    public boolean isDatabaseAvailable() {
        return isDatabaseAvailable.get();
    }

    /**
     * Индикатор состояния БД
     */
    public static class DatabaseHealthIndicator {
        private final DatabaseConfig databaseConfig;

        public DatabaseHealthIndicator(DatabaseConfig databaseConfig) {
            this.databaseConfig = databaseConfig;
        }

        public boolean isHealthy() {
            return databaseConfig.isDatabaseAvailable();
        }

        public String getStatus() {
            return isHealthy() ? "UP" : "DOWN";
        }
    }

    /**
     * DataSource обертка, которая проверяет доступность БД
     */
    private static class ResilientDataSource extends HikariDataSource {
        private final HikariDataSource delegate;
        private final DatabaseConfig config;

        public ResilientDataSource(HikariDataSource delegate, DatabaseConfig config) {
            this.delegate = delegate;
            this.config = config;
        }

        @Override
        public Connection getConnection() throws SQLException {
            if (!config.isDatabaseAvailable()) {
                throw new SQLException("База данных недоступна. Приложение работает в ограниченном режиме.");
            }
            return delegate.getConnection();
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            if (!config.isDatabaseAvailable()) {
                throw new SQLException("База данных недоступна. Приложение работает в ограниченном режиме.");
            }
            return delegate.getConnection(username, password);
        }

        @Override
        public void close() {
            delegate.close();
            config.scheduler.shutdown();
        }
    }
}