package com.bank.onlinebank.enums;

/**
 * Перечисление доступных гражданств для клиентов банка
 * Включает наиболее распространенные страны СНГ, Европы, Азии и Америки
 */
public enum Nationality {

    // СНГ и постсоветские страны
    RUSSIA("RU", "Россия", "Российская Федерация"),
    UKRAINE("UA", "Украина", "Украина"),
    BELARUS("BY", "Беларусь", "Республика Беларусь"),
    KAZAKHSTAN("KZ", "Казахстан", "Республика Казахстан"),
    UZBEKISTAN("UZ", "Узбекистан", "Республика Узбекистан"),
    TAJIKISTAN("TJ", "Таджикистан", "Республика Таджикистан"),
    KYRGYZSTAN("KG", "Киргизия", "Киргизская Республика"),
    ARMENIA("AM", "Армения", "Республика Армения"),
    AZERBAIJAN("AZ", "Азербайджан", "Азербайджанская Республика"),
    MOLDOVA("MD", "Молдова", "Республика Молдова"),
    GEORGIA("GE", "Грузия", "Грузия"),

    // Европа
    GERMANY("DE", "Германия", "Федеративная Республика Германия"),
    FRANCE("FR", "Франция", "Французская Республика"),
    ITALY("IT", "Италия", "Итальянская Республика"),
    SPAIN("ES", "Испания", "Королевство Испания"),
    POLAND("PL", "Польша", "Республика Польша"),
    UK("GB", "Великобритания", "Соединённое Королевство"),
    NETHERLANDS("NL", "Нидерланды", "Королевство Нидерландов"),
    BELGIUM("BE", "Бельгия", "Королевство Бельгия"),
    CZECH("CZ", "Чехия", "Чешская Республика"),
    AUSTRIA("AT", "Австрия", "Австрийская Республика"),
    SWITZERLAND("CH", "Швейцария", "Швейцарская Конфедерация"),
    SWEDEN("SE", "Швеция", "Королевство Швеция"),
    NORWAY("NO", "Норвегия", "Королевство Норвегия"),
    FINLAND("FI", "Финляндия", "Финляндская Республика"),
    DENMARK("DK", "Дания", "Королевство Дания"),

    // Азия
    CHINA("CN", "Китай", "Китайская Народная Республика"),
    JAPAN("JP", "Япония", "Япония"),
    SOUTH_KOREA("KR", "Южная Корея", "Республика Корея"),
    INDIA("IN", "Индия", "Республика Индия"),
    TURKEY("TR", "Турция", "Турецкая Республика"),
    ISRAEL("IL", "Израиль", "Государство Израиль"),
    UAE("AE", "ОАЭ", "Объединённые Арабские Эмираты"),
    SAUDI_ARABIA("SA", "Саудовская Аравия", "Королевство Саудовская Аравия"),
    SINGAPORE("SG", "Сингапур", "Республика Сингапур"),
    THAILAND("TH", "Таиланд", "Королевство Таиланд"),
    VIETNAM("VN", "Вьетнам", "Социалистическая Республика Вьетнам"),

    // Америка
    USA("US", "США", "Соединённые Штаты Америки"),
    CANADA("CA", "Канада", "Канада"),
    MEXICO("MX", "Мексика", "Мексиканские Соединённые Штаты"),
    BRAZIL("BR", "Бразилия", "Федеративная Республика Бразилия"),
    ARGENTINA("AR", "Аргентина", "Аргентинская Республика"),

    // Океания
    AUSTRALIA("AU", "Австралия", "Австралийский Союз"),
    NEW_ZEALAND("NZ", "Новая Зеландия", "Новая Зеландия"),

    // Другие
    OTHER("XX", "Другое", "Другое гражданство");

    private final String code;
    private final String shortName;
    private final String fullName;

    Nationality(String code, String shortName, String fullName) {
        this.code = code;
        this.shortName = shortName;
        this.fullName = fullName;
    }

    /**
     * Получить ISO код страны
     */
    public String getCode() {
        return code;
    }

    /**
     * Получить краткое название страны
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * Получить полное официальное название страны
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Получить название для отображения в UI
     */
    public String getDisplayName() {
        return shortName;
    }

    /**
     * Найти гражданство по коду страны
     */
    public static Nationality fromCode(String code) {
        for (Nationality nationality : values()) {
            if (nationality.code.equalsIgnoreCase(code)) {
                return nationality;
            }
        }
        return OTHER;
    }

    /**
     * Проверить, является ли гражданство из СНГ
     */
    public boolean isCIS() {
        return this == RUSSIA || this == UKRAINE || this == BELARUS ||
                this == KAZAKHSTAN || this == UZBEKISTAN || this == TAJIKISTAN ||
                this == KYRGYZSTAN || this == ARMENIA || this == AZERBAIJAN ||
                this == MOLDOVA;
    }

    /**
     * Проверить, является ли гражданство из ЕС
     */
    public boolean isEU() {
        return this == GERMANY || this == FRANCE || this == ITALY ||
                this == SPAIN || this == POLAND || this == NETHERLANDS ||
                this == BELGIUM || this == CZECH || this == AUSTRIA ||
                this == SWEDEN || this == FINLAND || this == DENMARK;
    }

    /**
     * Получить флаг страны (эмодзи)
     */
    public String getFlag() {
        if (code.equals("XX")) return "🏳️";

        int firstChar = Character.codePointAt("🇦", 0) - Character.codePointAt("A", 0);
        int firstFlag = Character.codePointAt(code, 0) + firstChar;
        int secondFlag = Character.codePointAt(code, 1) + firstChar;

        return new String(new int[]{firstFlag, secondFlag}, 0, 2);
    }

    @Override
    public String toString() {
        return shortName;
    }
}