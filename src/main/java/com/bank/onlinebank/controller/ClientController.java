package com.bank.onlinebank.controller;

import com.bank.onlinebank.dto.ClientDTO;
import com.bank.onlinebank.enums.Currency;
import com.bank.onlinebank.enums.Nationality;
import com.bank.onlinebank.service.ClientService;
import com.bank.onlinebank.service.DatabaseHealthService;
import com.bank.onlinebank.service.HealthCheckService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST контроллер для управления клиентами банка
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final HealthCheckService healthCheckService;
    private final DatabaseHealthService dbHealthService;

    /**
     * Получить список всех клиентов с пагинацией
     */
    @GetMapping
    @Timed(value = "api.clients.list", description = "Get all clients")
    public ResponseEntity<Page<ClientDTO>> getAllClients(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sort,
            @RequestParam(defaultValue = "ASC") String direction) {

        long startTime = System.currentTimeMillis();

        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<ClientDTO> clients = clientService.getAllClients(pageable);

            log.info("Получен список клиентов: страница {}, размер {}, всего элементов {}",
                    page, size, clients.getTotalElements());

            return ResponseEntity.ok(clients);

        } finally {
            healthCheckService.recordApiCall("/api/clients", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Поиск клиентов с фильтрами
     */
    @GetMapping("/search")
    @Timed(value = "api.clients.search", description = "Search clients")
    public ResponseEntity<Page<ClientDTO>> searchClients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) Nationality nationality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        long startTime = System.currentTimeMillis();

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("lastName"));

            Page<ClientDTO> clients = clientService.searchClients(
                    query, currency, nationality, pageable);

            log.info("Поиск клиентов: query='{}', currency={}, nationality={}, найдено {}",
                    query, currency, nationality, clients.getTotalElements());

            return ResponseEntity.ok(clients);

        } finally {
            healthCheckService.recordApiCall("/api/clients/search", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Получить клиента по ID
     */
    @GetMapping("/{id}")
    @Timed(value = "api.clients.get", description = "Get client by ID")
    public ResponseEntity<ClientDTO> getClient(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        try {
            ClientDTO client = clientService.getClientById(id);
            return ResponseEntity.ok(client);

        } finally {
            healthCheckService.recordApiCall("/api/clients/{id}", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Получить клиента по номеру счета
     */
    @GetMapping("/account/{accountNumber}")
    @Timed(value = "api.clients.getByAccount", description = "Get client by account number")
    public ResponseEntity<ClientDTO> getClientByAccount(@PathVariable String accountNumber) {
        long startTime = System.currentTimeMillis();

        try {
            ClientDTO client = clientService.getClientByAccountNumber(accountNumber);
            return ResponseEntity.ok(client);

        } finally {
            healthCheckService.recordApiCall("/api/clients/account/{accountNumber}",
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Создать нового клиента
     */
    @PostMapping
    @Timed(value = "api.clients.create", description = "Create new client")
    public ResponseEntity<ClientDTO> createClient(@Valid @RequestBody ClientDTO clientDTO) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Создание нового клиента: {} {}",
                    clientDTO.getLastName(), clientDTO.getFirstName());

            ClientDTO createdClient = clientService.createClient(clientDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(createdClient);

        } finally {
            healthCheckService.recordApiCall("/api/clients", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Обновить данные клиента
     */
    @PutMapping("/{id}")
    @Timed(value = "api.clients.update", description = "Update client")
    public ResponseEntity<ClientDTO> updateClient(
            @PathVariable Long id,
            @Valid @RequestBody ClientDTO clientDTO) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Обновление клиента с ID: {}", id);

            ClientDTO updatedClient = clientService.updateClient(id, clientDTO);

            return ResponseEntity.ok(updatedClient);

        } finally {
            healthCheckService.recordApiCall("/api/clients/{id}", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Удалить клиента
     */
    @DeleteMapping("/{id}")
    @Timed(value = "api.clients.delete", description = "Delete client")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Удаление клиента с ID: {}", id);

            clientService.deleteClient(id);

            return ResponseEntity.noContent().build();

        } finally {
            healthCheckService.recordApiCall("/api/clients/{id}", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Получить клиентов по возрасту
     */
    @GetMapping("/age")
    @Timed(value = "api.clients.byAge", description = "Get clients by age")
    public ResponseEntity<List<ClientDTO>> getClientsByAge(
            @RequestParam int minAge,
            @RequestParam int maxAge) {

        long startTime = System.currentTimeMillis();

        try {
            log.info("Получение клиентов по возрасту: {} - {} лет", minAge, maxAge);

            List<ClientDTO> clients = clientService.getClientsByAge(minAge, maxAge);

            return ResponseEntity.ok(clients);

        } finally {
            healthCheckService.recordApiCall("/api/clients/age", System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Получить статистику по валютам
     */
    @GetMapping("/statistics/currency")
    @Timed(value = "api.clients.statistics.currency", description = "Get currency statistics")
    public ResponseEntity<Map<Currency, Long>> getCurrencyStatistics() {
        long startTime = System.currentTimeMillis();

        try {
            Map<Currency, Long> statistics = clientService.getCurrencyStatistics();
            return ResponseEntity.ok(statistics);

        } finally {
            healthCheckService.recordApiCall("/api/clients/statistics/currency",
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Получить статистику по гражданству
     */
    @GetMapping("/statistics/nationality")
    @Timed(value = "api.clients.statistics.nationality", description = "Get nationality statistics")
    public ResponseEntity<Map<Nationality, Long>> getNationalityStatistics() {
        long startTime = System.currentTimeMillis();

        try {
            Map<Nationality, Long> statistics = clientService.getNationalityStatistics();
            return ResponseEntity.ok(statistics);

        } finally {
            healthCheckService.recordApiCall("/api/clients/statistics/nationality",
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Получить последних зарегистрированных клиентов
     */
    @GetMapping("/recent")
    @Timed(value = "api.clients.recent", description = "Get recent clients")
    public ResponseEntity<List<ClientDTO>> getRecentClients() {
        long startTime = System.currentTimeMillis();

        try {
            List<ClientDTO> clients = clientService.getRecentClients();
            return ResponseEntity.ok(clients);

        } finally {
            healthCheckService.recordApiCall("/api/clients/recent",
                    System.currentTimeMillis() - startTime);
        }
    }
}