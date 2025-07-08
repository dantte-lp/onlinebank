package com.bank.onlinebank.enums;

/**
 * Перечисление доступных валют для банковских счетов
 */
public enum Currency {

    RUB("RUB", "₽", "Российский рубль", "643"),
    USD("USD", "$", "Доллар США", "840"),
    EUR("EUR", "€", "Евро", "978");

    private final String code;
    private final String symbol;
    private final String displayName;
    private final String numericCode;

    Currency(String code, String symbol, String displayName, String numericCode) {
        this.code = code;
        this.symbol = symbol;
        this.displayName = displayName;
        this.numericCode = numericCode;
    }

    /**
     * Получить код валюты (например, USD)
     */
    public String getCode() {
        return code;
    }

    /**
     * Получить символ валюты (например, $)
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Получить полное название валюты
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Получить числовой код валюты по ISO 4217
     */
    public String getNumericCode() {
        return numericCode;
    }

    /**
     * Получить отображаемое название для UI
     */
    public String getLabel() {
        return String.format("%s (%s)", displayName, symbol);
    }

    /**
     * Форматировать сумму с символом валюты
     */
    public String formatAmount(double amount) {
        return String.format("%s %.2f", symbol, amount);
    }

    /**
     * Найти валюту по коду
     */
    public static Currency fromCode(String code) {
        for (Currency currency : values()) {
            if (currency.code.equalsIgnoreCase(code)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Неизвестный код валюты: " + code);
    }

    /**
     * Найти валюту по числовому коду
     */
    public static Currency fromNumericCode(String numericCode) {
        for (Currency currency : values()) {
            if (currency.numericCode.equals(numericCode)) {
                return currency;
            }
        }
        throw new IllegalArgumentException("Неизвестный числовой код валюты: " + numericCode);
    }

    @Override
    public String toString() {
        return displayName;
    }
}