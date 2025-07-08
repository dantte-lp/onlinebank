package com.bank.onlinebank.repository;

import com.bank.onlinebank.entity.Client;
import com.bank.onlinebank.enums.Currency;
import com.bank.onlinebank.enums.Nationality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository интерфейс для работы с сущностью Client
 */
@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {

    /**
     * Найти клиента по уникальному идентификатору.
     */
    Optional<Client> findByUniqueId(String uniqueId);

    /**
     * Найти клиента по номеру счета.
     */
    Optional<Client> findByAccountNumber(String accountNumber);

    /**
     * Найти клиента по номеру телефона.
     */
    Optional<Client> findByPhoneNumber(String phoneNumber);

    /**
     * Проверить существование клиента по номеру счета.
     */
    boolean existsByAccountNumber(String accountNumber);

    /**
     * Проверить существование клиента по номеру телефона.
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Проверить существование клиента по уникальному идентификатору.
     */
    boolean existsByUniqueId(String uniqueId);

    /**
     * Найти всех клиентов по валюте счета.
     */
    List<Client> findByCurrency(Currency currency);

    /**
     * Найти всех клиентов по гражданству.
     */
    List<Client> findByNationality(Nationality nationality);

    /**
     * Найти клиентов по фамилии (без учета регистра).
     */
    List<Client> findByLastNameIgnoreCase(String lastName);

    /**
     * Поиск клиентов по части ФИО (без учета регистра).
     */
    @Query("SELECT c FROM Client c WHERE " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.middleName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Client> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Комплексный поиск клиентов по различным критериям.
     */
    @Query("SELECT c FROM Client c WHERE " +
            "(:searchTerm IS NULL OR :searchTerm = '' OR " +
            "LOWER(c.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(c.middleName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "c.accountNumber LIKE CONCAT('%', :searchTerm, '%') OR " +
            "c.phoneNumber LIKE CONCAT('%', :searchTerm, '%')) AND " +
            "(:currency IS NULL OR c.currency = :currency) AND " +
            "(:nationality IS NULL OR c.nationality = :nationality)")
    Page<Client> searchClients(@Param("searchTerm") String searchTerm,
                               @Param("currency") Currency currency,
                               @Param("nationality") Nationality nationality,
                               Pageable pageable);

    /**
     * Найти клиентов, родившихся в определенном диапазоне дат.
     * Этот метод предпочтительнее для поиска по возрасту.
     */
    List<Client> findByBirthDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Получить количество клиентов по валютам.
     */
    @Query("SELECT c.currency, COUNT(c) FROM Client c GROUP BY c.currency")
    List<Object[]> countByCurrency();

    /**
     * Получить количество клиентов по гражданству.
     */
    @Query("SELECT c.nationality, COUNT(c) FROM Client c GROUP BY c.nationality ORDER BY COUNT(c) DESC")
    List<Object[]> countByNationality();

    /**
     * Найти последних 10 зарегистрированных клиентов.
     */
    List<Client> findTop10ByOrderByCreatedAtDesc();

    /**
     * Найти клиентов, обновленных после указанной даты.
     */
    @Query("SELECT c FROM Client c WHERE c.updatedAt > :date")
    List<Client> findRecentlyUpdated(@Param("date") LocalDate date);

    /**
     * Удалить клиентов, созданных до указанной даты (для очистки).
     */
    void deleteByCreatedAtBefore(LocalDate date);

    /**
     * Получить следующий доступный номер счета (PostgreSQL-specific).
     */
    @Query(value = "SELECT CONCAT('1', LPAD(CAST(COALESCE(MAX(CAST(SUBSTRING(account_number, 2) AS BIGINT)), 0) + 1 AS VARCHAR), 19, '0')) " +
            "FROM clients WHERE account_number ~ '^1[0-9]{19}'", nativeQuery = true)
    String getNextAccountNumber();
}