package com.bank.onlinebank.service;

import com.bank.onlinebank.dto.ClientDTO;
import com.bank.onlinebank.entity.Client;
import com.bank.onlinebank.enums.Currency;
import com.bank.onlinebank.enums.Nationality;
import com.bank.onlinebank.exception.ClientAlreadyExistsException;
import com.bank.onlinebank.exception.ClientNotFoundException;
import com.bank.onlinebank.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для управления клиентами банка
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;

    /**
     * Получить всех клиентов с пагинацией
     */
    public Page<ClientDTO> getAllClients(Pageable pageable) {
        log.debug("Получение списка клиентов, страница: {}, размер: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<Client> clients = clientRepository.findAll(pageable);
        return clients.map(this::convertToDTO);
    }

    /**
     * Получить клиента по ID
     */
    public ClientDTO getClientById(Long id) {
        log.debug("Получение клиента по ID: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Клиент с ID " + id + " не найден"));

        return convertToDTO(client);
    }

    /**
     * Получить клиента по уникальному идентификатору
     */
    public ClientDTO getClientByUniqueId(String uniqueId) {
        log.debug("Получение клиента по уникальному ID: {}", uniqueId);

        Client client = clientRepository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new ClientNotFoundException("Клиент с уникальным ID " + uniqueId + " не найден"));

        return convertToDTO(client);
    }

    /**
     * Получить клиента по номеру счета
     */
    public ClientDTO getClientByAccountNumber(String accountNumber) {
        log.debug("Получение клиента по номеру счета: {}", accountNumber);

        Client client = clientRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ClientNotFoundException("Клиент с номером счета " + accountNumber + " не найден"));

        return convertToDTO(client);
    }

    /**
     * Поиск клиентов с фильтрами
     */
    public Page<ClientDTO> searchClients(String searchTerm, Currency currency,
                                         Nationality nationality, Pageable pageable) {
        log.debug("Поиск клиентов: searchTerm={}, currency={}, nationality={}",
                searchTerm, currency, nationality);

        Page<Client> clients = clientRepository.searchClients(
                searchTerm, currency, nationality, pageable);

        return clients.map(this::convertToDTO);
    }

    /**
     * Создать нового клиента
     */
    @Transactional
    public ClientDTO createClient(ClientDTO clientDTO) {
        log.info("Создание нового клиента: {} {}", clientDTO.getLastName(), clientDTO.getFirstName());

        // Проверка уникальности
        validateUniqueFields(clientDTO);

        Client client = convertToEntity(clientDTO);

        // Генерация уникального ID если не указан
        if (client.getUniqueId() == null || client.getUniqueId().isEmpty()) {
            client.setUniqueId(UUID.randomUUID().toString());
        }

        // Генерация номера счета если не указан
        if (client.getAccountNumber() == null || client.getAccountNumber().isEmpty()) {
            client.setAccountNumber(generateAccountNumber());
        }

        Client savedClient = clientRepository.save(client);
        log.info("Клиент успешно создан с ID: {}", savedClient.getId());

        return convertToDTO(savedClient);
    }

    /**
     * Обновить данные клиента
     */
    @Transactional
    public ClientDTO updateClient(Long id, ClientDTO clientDTO) {
        log.info("Обновление клиента с ID: {}", id);

        Client existingClient = clientRepository.findById(id)
                .orElseThrow(() -> new ClientNotFoundException("Клиент с ID " + id + " не найден"));

        // Проверка уникальности при изменении
        validateUniqueFieldsForUpdate(clientDTO, existingClient);

        // Обновляем поля
        updateClientFields(existingClient, clientDTO);

        Client savedClient = clientRepository.save(existingClient);
        log.info("Клиент с ID {} успешно обновлен", id);

        return convertToDTO(savedClient);
    }

    /**
     * Удалить клиента
     */
    @Transactional
    public void deleteClient(Long id) {
        log.info("Удаление клиента с ID: {}", id);

        if (!clientRepository.existsById(id)) {
            throw new ClientNotFoundException("Клиент с ID " + id + " не найден");
        }

        clientRepository.deleteById(id);
        log.info("Клиент с ID {} успешно удален", id);
    }

    /**
     * Получить клиентов по возрасту
     */
    public List<ClientDTO> getClientsByAge(int minAge, int maxAge) {
        log.debug("Получение клиентов по возрасту: {} - {} лет", minAge, maxAge);

        LocalDate endDate = LocalDate.now().minusYears(minAge);
        LocalDate startDate = LocalDate.now().minusYears(maxAge + 1).plusDays(1);

        List<Client> clients = clientRepository.findByBirthDateBetween(startDate, endDate);
        return clients.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Получить статистику по валютам
     */
    public Map<Currency, Long> getCurrencyStatistics() {
        log.debug("Получение статистики по валютам");

        List<Object[]> stats = clientRepository.countByCurrency();
        Map<Currency, Long> result = new HashMap<>();

        for (Object[] stat : stats) {
            Currency currency = (Currency) stat[0];
            Long count = (Long) stat[1];
            result.put(currency, count);
        }

        return result;
    }

    /**
     * Получить статистику по гражданству
     */
    public Map<Nationality, Long> getNationalityStatistics() {
        log.debug("Получение статистики по гражданству");

        List<Object[]> stats = clientRepository.countByNationality();
        Map<Nationality, Long> result = new LinkedHashMap<>(); // Сохраняем порядок

        for (Object[] stat : stats) {
            Nationality nationality = (Nationality) stat[0];
            Long count = (Long) stat[1];
            result.put(nationality, count);
        }

        return result;
    }

    /**
     * Получить последних зарегистрированных клиентов
     */
    public List<ClientDTO> getRecentClients() {
        log.debug("Получение последних зарегистрированных клиентов");

        List<Client> clients = clientRepository.findTop10ByOrderByCreatedAtDesc();
        return clients.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Проверка уникальности полей при создании
     */
    private void validateUniqueFields(ClientDTO clientDTO) {
        if (clientDTO.getAccountNumber() != null &&
                clientRepository.existsByAccountNumber(clientDTO.getAccountNumber())) {
            throw new ClientAlreadyExistsException("Клиент с номером счета " +
                    clientDTO.getAccountNumber() + " уже существует");
        }

        if (clientDTO.getPhoneNumber() != null &&
                clientRepository.existsByPhoneNumber(clientDTO.getPhoneNumber())) {
            throw new ClientAlreadyExistsException("Клиент с номером телефона " +
                    clientDTO.getPhoneNumber() + " уже существует");
        }
    }

    /**
     * Проверка уникальности полей при обновлении
     */
    private void validateUniqueFieldsForUpdate(ClientDTO clientDTO, Client existingClient) {
        // Проверяем номер счета, если он изменился
        if (!existingClient.getAccountNumber().equals(clientDTO.getAccountNumber()) &&
                clientRepository.existsByAccountNumber(clientDTO.getAccountNumber())) {
            throw new ClientAlreadyExistsException("Клиент с номером счета " +
                    clientDTO.getAccountNumber() + " уже существует");
        }

        // Проверяем телефон, если он изменился
        if (!existingClient.getPhoneNumber().equals(clientDTO.getPhoneNumber()) &&
                clientRepository.existsByPhoneNumber(clientDTO.getPhoneNumber())) {
            throw new ClientAlreadyExistsException("Клиент с номером телефона " +
                    clientDTO.getPhoneNumber() + " уже существует");
        }
    }

    /**
     * Генерация нового номера счета
     */
    private String generateAccountNumber() {
        String accountNumber = clientRepository.getNextAccountNumber();
        if (accountNumber == null) {
            // Fallback если база пустая
            accountNumber = "10000000000000000001";
        }
        return accountNumber;
    }

    /**
     * Обновление полей клиента
     */
    private void updateClientFields(Client client, ClientDTO dto) {
        client.setLastName(dto.getLastName());
        client.setFirstName(dto.getFirstName());
        client.setMiddleName(dto.getMiddleName());
        client.setBirthDate(dto.getBirthDate());
        client.setCurrency(dto.getCurrency());
        client.setNationality(dto.getNationality());
        client.setPhoneNumber(dto.getPhoneNumber());

        // Номер счета и уникальный ID обычно не меняются
        if (dto.getAccountNumber() != null && !dto.getAccountNumber().equals(client.getAccountNumber())) {
            client.setAccountNumber(dto.getAccountNumber());
        }
    }

    /**
     * Конвертация Entity в DTO
     */
    private ClientDTO convertToDTO(Client client) {
        return ClientDTO.builder()
                .id(client.getId())
                .uniqueId(client.getUniqueId())
                .lastName(client.getLastName())
                .firstName(client.getFirstName())
                .middleName(client.getMiddleName())
                .fullName(client.getFullName())
                .shortName(client.getShortName())
                .birthDate(client.getBirthDate())
                .age(client.getAge())
                .accountNumber(client.getAccountNumber())
                .currency(client.getCurrency())
                .nationality(client.getNationality())
                .phoneNumber(client.getPhoneNumber())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }

    /**
     * Конвертация DTO в Entity
     */
    private Client convertToEntity(ClientDTO dto) {
        Client client = new Client();
        client.setUniqueId(dto.getUniqueId());
        client.setLastName(dto.getLastName());
        client.setFirstName(dto.getFirstName());
        client.setMiddleName(dto.getMiddleName());
        client.setBirthDate(dto.getBirthDate());
        client.setAccountNumber(dto.getAccountNumber());
        client.setCurrency(dto.getCurrency());
        client.setNationality(dto.getNationality());
        client.setPhoneNumber(dto.getPhoneNumber());

        return client;
    }
}