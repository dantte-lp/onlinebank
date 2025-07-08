package com.bank.onlinebank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Сервис для активного мониторинга состояния подключения к базе данных.
 * <p>
 * Этот сервис инкапсулирует логику проверки доступности БД. Он использует
 * фоновую задачу (@Scheduled) для периодической проверки и хранит текущий
 * статус в потокобезопасной переменной.
 * </p>
 */
@Service
public class DatabaseHealthService {

    private final DataSource dataSource;
    // Используем AtomicBoolean для потокобезопасного хранения статуса
    private final AtomicBoolean isDbConnected = new AtomicBoolean(false);
    private String databaseVersion;

    @Autowired
    public DatabaseHealthService(DataSource dataSource) {
        this.dataSource = dataSource;
        // Первичная проверка при инициализации
        checkConnection();
    }

    /**
     * Возвращает true, если база данных доступна.
     */
    public boolean isDatabaseConnected() {
        return isDbConnected.get();
    }

    /**
     * Возвращает версию СУБД, если подключение успешно установлено.
     */
    public String getDatabaseVersion() {
        return isDatabaseConnected() ? databaseVersion : "N/A";
    }

    /**
     * Периодическая проверка соединения с БД.
     * Запускается каждые 15 секунд. При первой успешной попытке
     * получает и кэширует версию БД.
     */
    @Scheduled(fixedRate = 15000) // Проверять каждые 15 секунд
    public void checkConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) { // 2 секунды таймаут
                if (!isDbConnected.get()) {
                    // Если мы только что подключились, получаем версию БД
                    this.databaseVersion = connection.getMetaData().getDatabaseProductVersion();
                    System.out.println("Успешное подключение к БД. Версия: " + this.databaseVersion);
                }
                isDbConnected.set(true);
            } else {
                handleConnectionLoss();
            }
        } catch (SQLException e) {
            handleConnectionLoss();
        }
    }

    private void handleConnectionLoss() {
        if (isDbConnected.get()) {
            System.err.println("Потеряно соединение с базой данных.");
        }
        isDbConnected.set(false);
        this.databaseVersion = null;
    }
}