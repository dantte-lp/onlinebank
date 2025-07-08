package com.bank.onlinebank.exception;

/**
 * Исключение, выбрасываемое когда клиент с такими данными уже существует
 */
public class ClientAlreadyExistsException extends RuntimeException {

    public ClientAlreadyExistsException(String message) {
        super(message);
    }

    public ClientAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
