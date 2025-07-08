package com.bank.onlinebank.entity;

import com.bank.onlinebank.enums.Currency;
import com.bank.onlinebank.enums.Nationality;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity класс, представляющий клиента банка
 */
@Entity
@Table(name = "clients", indexes = {
        @Index(name = "idx_unique_id", columnList = "uniqueId"),
        @Index(name = "idx_last_name", columnList = "lastName"),
        @Index(name = "idx_account_number", columnList = "accountNumber"),
        @Index(name = "idx_phone_number", columnList = "phoneNumber")
})
@EntityListeners(AuditingEntityListener.class)
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    @NotBlank(message = "Уникальный идентификатор не может быть пустым")
    private String uniqueId;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Фамилия обязательна для заполнения")
    @Size(min = 2, max = 100, message = "Фамилия должна содержать от 2 до 100 символов")
    @Pattern(regexp = "^[А-Яа-яЁё\\s-]+$", message = "Фамилия должна содержать только русские буквы")
    private String lastName;

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Имя обязательно для заполнения")
    @Size(min = 2, max = 100, message = "Имя должно содержать от 2 до 100 символов")
    @Pattern(regexp = "^[А-Яа-яЁё\\s-]+$", message = "Имя должно содержать только русские буквы")
    private String firstName;

    @Column(length = 100)
    @Size(max = 100, message = "Отчество не должно превышать 100 символов")
    @Pattern(regexp = "^[А-Яа-яЁё\\s-]*$", message = "Отчество должно содержать только русские буквы")
    private String middleName;

    @Column(nullable = false)
    @NotNull(message = "Дата рождения обязательна для заполнения")
    @Past(message = "Дата рождения должна быть в прошлом")
    private LocalDate birthDate;

    @Column(nullable = false, unique = true, length = 20)
    @NotBlank(message = "Номер счета обязателен для заполнения")
    @Pattern(regexp = "^\\d{20}$", message = "Номер счета должен состоять из 20 цифр")
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @NotNull(message = "Валюта счета обязательна для выбора")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @NotNull(message = "Гражданство обязательно для выбора")
    private Nationality nationality;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Номер телефона обязателен для заполнения")
    @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "Номер телефона должен быть в международном формате E.164 (например: +1234567890)")
    private String phoneNumber;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    // Конструкторы
    public Client() {
    }

    public Client(String uniqueId, String lastName, String firstName, String middleName,
                  LocalDate birthDate, String accountNumber, Currency currency,
                  Nationality nationality, String phoneNumber) {
        this.uniqueId = uniqueId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.birthDate = birthDate;
        this.accountNumber = accountNumber;
        this.currency = currency;
        this.nationality = nationality;
        this.phoneNumber = phoneNumber;
    }

    // Вспомогательные методы
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Получить полное имя клиента
     */
    @Transient
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        fullName.append(lastName).append(" ").append(firstName);
        if (middleName != null && !middleName.trim().isEmpty()) {
            fullName.append(" ").append(middleName);
        }
        return fullName.toString();
    }

    /**
     * Получить краткое имя клиента (Фамилия И.О.)
     */
    @Transient
    public String getShortName() {
        StringBuilder shortName = new StringBuilder();
        shortName.append(lastName).append(" ").append(firstName.charAt(0)).append(".");
        if (middleName != null && !middleName.trim().isEmpty()) {
            shortName.append(middleName.charAt(0)).append(".");
        }
        return shortName.toString();
    }

    /**
     * Получить возраст клиента
     */
    @Transient
    public int getAge() {
        return LocalDate.now().getYear() - birthDate.getYear();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public Nationality getNationality() {
        return nationality;
    }

    public void setNationality(Nationality nationality) {
        this.nationality = nationality;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Client client = (Client) o;
        return Objects.equals(uniqueId, client.uniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uniqueId);
    }

    @Override
    public String toString() {
        return "Client{" +
                "id=" + id +
                ", uniqueId='" + uniqueId + '\'' +
                ", fullName='" + getFullName() + '\'' +
                ", birthDate=" + birthDate +
                ", accountNumber='" + accountNumber + '\'' +
                ", currency=" + currency +
                ", nationality=" + nationality +
                ", phoneNumber='" + phoneNumber + '\'' +
                '}';
    }
}