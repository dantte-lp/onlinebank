package com.bank.onlinebank;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Главный класс приложения Online Bank
 */
@Slf4j
@SpringBootApplication
public class OnlinebankApplication {

    private final Environment env;

    public OnlinebankApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(OnlinebankApplication.class, args);
    }

    /**
     * Вывод информации о запуске приложения
     */
    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }

        String serverPort = env.getProperty("server.port", "8080");
        String contextPath = env.getProperty("server.servlet.context-path", "");
        String hostAddress = "localhost";

        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("Не удалось определить адрес хоста, используется localhost");
        }

        log.info("\n----------------------------------------------------------\n\t" +
                        "Приложение '{}' запущено успешно! Доступные URL:\n\t" +
                        "Локальный: \t\t{}://localhost:{}{}\n\t" +
                        "Внешний: \t\t{}://{}:{}{}\n\t" +
                        "Профиль(и): \t\t{}\n\t" +
                        "Health Check: \t{}://localhost:{}{}/health\n\t" +
                        "API Docs: \t\t{}://localhost:{}{}/api/health/detailed\n" +
                        "----------------------------------------------------------",
                env.getProperty("spring.application.name"),
                protocol, serverPort, contextPath,
                protocol, hostAddress, serverPort, contextPath,
                env.getActiveProfiles().length > 0 ? env.getActiveProfiles() : new String[]{"default"},
                protocol, serverPort, contextPath,
                protocol, serverPort, contextPath
        );

        // Предупреждение о базе данных
        String dbUrl = env.getProperty("spring.datasource.url");
        if (dbUrl != null) {
            log.info("База данных: {}", dbUrl);
        } else {
            log.warn("URL базы данных не настроен!");
        }
    }
}