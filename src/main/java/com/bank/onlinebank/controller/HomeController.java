package com.bank.onlinebank.controller;

import com.bank.onlinebank.config.DatabaseConfig;
import com.bank.onlinebank.dto.ClientDTO;
import com.bank.onlinebank.enums.Currency;
import com.bank.onlinebank.enums.Nationality;
import com.bank.onlinebank.service.ClientService;
import com.bank.onlinebank.service.HealthCheckService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Контроллер для веб-интерфейса приложения
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ClientService clientService;
    private final HealthCheckService healthCheckService;
    private final DatabaseConfig.DatabaseHealthIndicator databaseHealthIndicator;

    /**
     * Главная страница - перенаправление на список клиентов
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/clients";
    }

    /**
     * Список клиентов с поиском и пагинацией
     */
    @GetMapping("/clients")
    public String listClients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Currency currency,
            @RequestParam(required = false) Nationality nationality,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "lastName") String sort,
            @RequestParam(defaultValue = "ASC") String direction,
            Model model) {

        log.debug("Отображение списка клиентов: page={}, search={}", page, search);

        // Проверяем доступность БД
        if (!databaseHealthIndicator.isHealthy()) {
            model.addAttribute("dbError", true);
            model.addAttribute("errorMessage", "База данных недоступна. Приложение работает в ограниченном режиме.");
            model.addAttribute("clients", Page.empty()); // Пустая страница
            model.addAttribute("totalItems", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("currentPage", 0);
            model.addAttribute("pageNumbers", new int[0]);

            // Списки для фильтров
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("nationalities", getNationalitiesGrouped());

            return "clients/list";
        }

        try {
            Sort.Direction sortDirection = Sort.Direction.fromString(direction);
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            Page<ClientDTO> clients = clientService.searchClients(search, currency, nationality, pageable);

            // Добавляем данные для пагинации
            model.addAttribute("clients", clients);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", clients.getTotalPages());
            model.addAttribute("totalItems", clients.getTotalElements());
            model.addAttribute("pageNumbers", getPageNumbers(clients));

            // Параметры поиска и сортировки
            model.addAttribute("search", search);
            model.addAttribute("selectedCurrency", currency);
            model.addAttribute("selectedNationality", nationality);
            model.addAttribute("sort", sort);
            model.addAttribute("direction", direction);

            // Списки для фильтров
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("nationalities", getNationalitiesGrouped());

            // Статистика
            model.addAttribute("currencyStats", clientService.getCurrencyStatistics());

        } catch (Exception e) {
            log.error("Ошибка при получении списка клиентов", e);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Произошла ошибка при загрузке данных");
        }

        return "clients/list";
    }

    /**
     * Форма создания нового клиента (модальное окно)
     */
    @GetMapping("/clients/new")
    @ResponseBody
    public String newClientForm(Model model) {
        ClientDTO client = new ClientDTO();
        model.addAttribute("client", client);
        model.addAttribute("currencies", Currency.values());
        model.addAttribute("nationalities", Arrays.asList(Nationality.values()));

        return "fragments/client-form :: clientForm";
    }

    /**
     * Создание нового клиента
     */
    @PostMapping("/clients")
    public String createClient(@Valid @ModelAttribute("client") ClientDTO client,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            log.warn("Ошибки валидации при создании клиента: {}", result.getAllErrors());
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Проверьте правильность заполнения формы");
            return "redirect:/clients";
        }

        try {
            ClientDTO createdClient = clientService.createClient(client);
            log.info("Создан новый клиент: {} {}", createdClient.getLastName(), createdClient.getFirstName());

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage",
                    String.format("Клиент %s успешно создан", createdClient.getFullName()));

        } catch (Exception e) {
            log.error("Ошибка при создании клиента", e);
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при создании клиента: " + e.getMessage());
        }

        return "redirect:/clients";
    }

    /**
     * Страница детальной информации о клиенте
     */
    @GetMapping("/clients/{id}")
    public String viewClient(@PathVariable Long id, Model model) {
        try {
            ClientDTO client = clientService.getClientById(id);
            model.addAttribute("client", client);
            return "clients/view";
        } catch (Exception e) {
            log.error("Ошибка при получении клиента с ID: {}", id, e);
            return "redirect:/clients";
        }
    }

    /**
     * Форма редактирования клиента
     */
    @GetMapping("/clients/{id}/edit")
    public String editClientForm(@PathVariable Long id, Model model) {
        try {
            ClientDTO client = clientService.getClientById(id);
            model.addAttribute("client", client);
            model.addAttribute("currencies", Currency.values());
            model.addAttribute("nationalities", Arrays.asList(Nationality.values()));
            return "clients/edit";
        } catch (Exception e) {
            log.error("Ошибка при получении клиента для редактирования: {}", id, e);
            return "redirect:/clients";
        }
    }

    /**
     * Обновление данных клиента
     */
    @PostMapping("/clients/{id}")
    public String updateClient(@PathVariable Long id,
                               @Valid @ModelAttribute("client") ClientDTO client,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Проверьте правильность заполнения формы");
            return "redirect:/clients/" + id + "/edit";
        }

        try {
            ClientDTO updatedClient = clientService.updateClient(id, client);

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Данные клиента успешно обновлены");

            return "redirect:/clients/" + id;

        } catch (Exception e) {
            log.error("Ошибка при обновлении клиента", e);
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/clients/" + id + "/edit";
        }
    }

    /**
     * Удаление клиента
     */
    @PostMapping("/clients/{id}/delete")
    public String deleteClient(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            clientService.deleteClient(id);

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("successMessage", "Клиент успешно удален");

        } catch (Exception e) {
            log.error("Ошибка при удалении клиента", e);
            redirectAttributes.addFlashAttribute("error", true);
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка при удалении: " + e.getMessage());
        }

        return "redirect:/clients";
    }

    /**
     * Страница статуса системы (Health Check)
     */
    @GetMapping("/health")
    public String healthStatus(Model model) {
        try {
            var healthStatus = healthCheckService.getHealthStatus();
            model.addAttribute("health", healthStatus);
            model.addAttribute("isHealthy", healthStatus.isHealthy());
            model.addAttribute("isDegraded", healthStatus.isDegraded());

        } catch (Exception e) {
            log.error("Ошибка при получении статуса системы", e);
            model.addAttribute("error", true);
            model.addAttribute("errorMessage", "Не удалось получить статус системы");
        }

        return "health/status";
    }

    /**
     * Обработка ошибок 404
     */
    @GetMapping("/error/404")
    public String notFound() {
        return "error/404";
    }

    /**
     * Обработка ошибок 500
     */
    @GetMapping("/error/500")
    public String serverError() {
        return "error/500";
    }

    /**
     * Вспомогательный метод для генерации номеров страниц
     */
    private int[] getPageNumbers(Page<?> page) {
        int totalPages = page.getTotalPages();
        if (totalPages <= 7) {
            return IntStream.rangeClosed(0, totalPages - 1).toArray();
        }

        int currentPage = page.getNumber();
        int start = Math.max(0, currentPage - 3);
        int end = Math.min(totalPages - 1, currentPage + 3);

        if (currentPage <= 3) {
            end = 6;
        } else if (currentPage >= totalPages - 4) {
            start = totalPages - 7;
        }

        return IntStream.rangeClosed(start, end).toArray();
    }

    /**
     * Группировка национальностей по регионам для удобного отображения
     */
    private Map<String, java.util.List<Nationality>> getNationalitiesGrouped() {
        return Map.of(
                "СНГ", Arrays.stream(Nationality.values())
                        .filter(Nationality::isCIS)
                        .collect(Collectors.toList()),
                "Европа", Arrays.stream(Nationality.values())
                        .filter(Nationality::isEU)
                        .collect(Collectors.toList()),
                "Другие", Arrays.stream(Nationality.values())
                        .filter(n -> !n.isCIS() && !n.isEU())
                        .collect(Collectors.toList())
        );
    }
}