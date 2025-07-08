package com.bank.onlinebank.config;

import com.bank.onlinebank.entity.Client;
import com.bank.onlinebank.enums.Currency;
import com.bank.onlinebank.enums.Nationality;
import com.bank.onlinebank.repository.ClientRepository;
import com.bank.onlinebank.util.ClientDataGenerator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Компонент для инициализации базы данных тестовыми данными
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@DependsOn("databaseHealthIndicator")
public class DataInitializer {

    private final ClientRepository clientRepository;
    private final DatabaseConfig.DatabaseHealthIndicator databaseHealthIndicator;
    private final ClientDataGenerator dataGenerator;

    @Value("${app.data.init.enabled:true}")
    private boolean initEnabled;

    @Value("${app.data.init.client-count:100}")
    private int clientCount;

    @Value("${app.data.init.clean-before:false}")
    private boolean cleanBefore;

    private static final Random random = new Random();

    @PostConstruct
    public void init() {
        if (!databaseHealthIndicator.isHealthy()) {
            log.warn("База данных недоступна. Пропускаем инициализацию данных");
            return;
        }

        // Ждем немного, чтобы схема успела создаться
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            // Инициализируем данные
            if (initEnabled) {
                initializeData();
            }
        } catch (Exception e) {
            log.error("Ошибка при инициализации: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void initializeData() {
        log.info("Начало инициализации тестовых данных");

        // Проверяем, есть ли уже данные
        long existingCount = clientRepository.count();
        if (existingCount > 0 && !cleanBefore) {
            log.info("База данных уже содержит {} клиентов. Пропускаем инициализацию", existingCount);
            return;
        }

        if (cleanBefore && existingCount > 0) {
            log.info("Очистка существующих данных...");
            clientRepository.deleteAll();
        }

        log.info("Создание {} тестовых клиентов...", clientCount);

        List<Client> clients = new ArrayList<>();

        // Генерируем базовый номер счета из 19 цифр, затем преобразуем в строку и добавляем 20-ю цифру
        String accountNumberPrefix = "1000000000000000000"; // 19 цифр

        for (int i = 0; i < clientCount; i++) {
            try {
                // Генерируем 20-значный номер счета как строку
                java.math.BigInteger baseAccountNumber = new java.math.BigInteger("10000000000000000000");
                String accountNumber = baseAccountNumber.add(java.math.BigInteger.valueOf(i)).toString();
                Client client = createRandomClient(accountNumber);
                clients.add(client);

                // Сохраняем батчами по 20 записей
                if (clients.size() == 20) {
                    clientRepository.saveAll(clients);
                    clients.clear();
                    log.debug("Сохранено {} клиентов", i + 1);
                }
            } catch (Exception e) {
                log.error("Ошибка при создании клиента #{}: {}", i, e.getMessage());
            }
        }

        // Сохраняем оставшиеся записи
        if (!clients.isEmpty()) {
            clientRepository.saveAll(clients);
        }

        log.info("Инициализация завершена. Создано {} клиентов", clientRepository.count());
    }

    private Client createRandomClient(String accountNumber) {
        Client client = new Client();

        // Уникальный идентификатор
        client.setUniqueId(UUID.randomUUID().toString());

        // Генерируем ФИО
        ClientDataGenerator.PersonName name = dataGenerator.generateRandomName();
        client.setLastName(name.lastName());
        client.setFirstName(name.firstName());
        client.setMiddleName(name.middleName());

        // Дата рождения (от 18 до 80 лет)
        int age = 18 + random.nextInt(63);
        LocalDate birthDate = LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
        client.setBirthDate(birthDate);

        // Номер счета
        client.setAccountNumber(accountNumber);

        // Валюта (распределение: 60% RUB, 25% USD, 15% EUR)
        Currency currency = selectRandomCurrency();
        client.setCurrency(currency);

        // Гражданство (распределение с учетом реальности)
        Nationality nationality = selectRandomNationality();
        client.setNationality(nationality);

        // Телефон
        client.setPhoneNumber(dataGenerator.generatePhoneNumber(nationality));

        return client;
    }

    private Currency selectRandomCurrency() {
        int rand = random.nextInt(100);
        if (rand < 60) {
            return Currency.RUB;
        } else if (rand < 85) {
            return Currency.USD;
        } else {
            return Currency.EUR;
        }
    }

    private Nationality selectRandomNationality() {
        // Взвешенное распределение для реалистичности
        Nationality[] commonNationalities = {
                Nationality.RUSSIA, Nationality.RUSSIA, Nationality.RUSSIA, // 30%
                Nationality.KAZAKHSTAN, Nationality.KAZAKHSTAN, // 20%
                Nationality.UZBEKISTAN, Nationality.UZBEKISTAN, // 20%
                Nationality.BELARUS, // 10%
                Nationality.UKRAINE, // 10%
                Nationality.ARMENIA, // 5%
                Nationality.KYRGYZSTAN // 5%
        };

        // 80% - страны СНГ, 20% - другие
        if (random.nextInt(100) < 80) {
            return commonNationalities[random.nextInt(commonNationalities.length)];
        } else {
            // Случайная страна из всех доступных
            Nationality[] all = Nationality.values();
            return all[random.nextInt(all.length)];
        }
    }
}