package com.bank.onlinebank.exception;

/**
 * Исключение, выбрасываемое когда клиент не найден
 */
public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(String message) {
        super(message);
    }

    public ClientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClientNotFoundException(Long clientId) {
        super("Клиент с ID " + clientId + " не найден");
    }
}