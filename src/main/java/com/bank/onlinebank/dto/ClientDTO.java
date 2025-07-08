package com.bank.onlinebank.dto;

import com.bank.onlinebank.enums.Currency;
import com.bank.onlinebank.enums.Nationality;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO для передачи данных клиента между слоями приложения
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientDTO {

    private Long id;

    private String uniqueId;

    @NotBlank(message = "Фамилия обязательна для заполнения")
    @Size(min = 2, max = 100, message = "Фамилия должна содержать от 2 до 100 символов")
    @Pattern(regexp = "^[\\p{L}\\s'-]+$", message = "Фамилия должна содержать только русские буквы")
    private String lastName;

    @NotBlank(message = "Имя обязательно для заполнения")
    @Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов")
    @Pattern(regexp = "^[\\p{L}\\s'-]+$", message = "Имя должно содержать только русские буквы")
    private String firstName;

    @Size(max = 100, message = "Отчество не должно превышать 100 символов")
    @Pattern(regexp = "^[\\p{L}\\s'-]+$", message = "Отчество должно содержать только русские буквы")
    private String middleName;

    // Вычисляемые поля для удобства отображения
    private String fullName;
    private String shortName;

    @NotNull(message = "Дата рождения обязательна для заполнения")
    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthDate;

    // Вычисляемое поле
    private Integer age;

    @Pattern(regexp = "^\\d{20}$", message = "Номер счета должен состоять из 20 цифр")
    private String accountNumber;

    @NotNull(message = "Валюта счета обязательна для выбора")
    private Currency currency;

    @NotNull(message = "Гражданство обязательно для выбора")
    private Nationality nationality;

    @NotBlank(message = "Номер телефона обязателен для заполнения")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Номер телефона должен быть в международном формате E.164")
    private String phoneNumber;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * Получить отображаемое имя валюты с символом
     */
    public String getCurrencyDisplay() {
        return currency != null ? currency.getLabel() : "";
    }

    /**
     * Получить отображаемое название гражданства с флагом
     */
    public String getNationalityDisplay() {
        return nationality != null ?
                nationality.getFlag() + " " + nationality.getDisplayName() : "";
    }

    /**
     * Форматированный номер счета для отображения
     */
    public String getFormattedAccountNumber() {
        if (accountNumber == null || accountNumber.length() != 20) {
            return accountNumber;
        }
        // Форматируем как XXXX XXXX XXXX XXXX XXXX
        return accountNumber.replaceAll("(.{4})", "$1 ").trim();
    }

    /**
     * Проверка, является ли клиент из СНГ
     */
    public boolean isCISCitizen() {
        return nationality != null && nationality.isCIS();
    }

    /**
     * Проверка, является ли клиент из ЕС
     */
    public boolean isEUCitizen() {
        return nationality != null && nationality.isEU();
    }
}