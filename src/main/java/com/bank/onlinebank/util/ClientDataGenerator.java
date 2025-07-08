package com.bank.onlinebank.util;

import com.bank.onlinebank.enums.Nationality;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * Утилита для генерации реалистичных тестовых данных клиентов
 */
@Component
public class ClientDataGenerator {

    private static final Random random = new Random();

    // Русские имена
    private static final String[] RUSSIAN_MALE_FIRST_NAMES = {
            "Александр", "Сергей", "Владимир", "Андрей", "Алексей", "Дмитрий", "Михаил",
            "Иван", "Максим", "Николай", "Евгений", "Павел", "Артем", "Виктор", "Константин",
            "Игорь", "Олег", "Роман", "Денис", "Антон", "Илья", "Юрий", "Григорий", "Василий",
            "Петр", "Егор", "Георгий", "Кирилл", "Арсений", "Леонид"
    };

    private static final String[] RUSSIAN_FEMALE_FIRST_NAMES = {
            "Елена", "Ольга", "Наталья", "Татьяна", "Ирина", "Светлана", "Марина", "Анна",
            "Людмила", "Екатерина", "Мария", "Галина", "Валентина", "Надежда", "Юлия",
            "Александра", "Любовь", "Лариса", "Вера", "Алина", "Дарья", "Анастасия",
            "Виктория", "Ксения", "Полина", "София", "Алиса", "Евгения", "Вероника", "Маргарита"
    };

    private static final String[] RUSSIAN_MALE_MIDDLE_NAMES = {
            "Александрович", "Сергеевич", "Владимирович", "Андреевич", "Алексеевич",
            "Дмитриевич", "Михайлович", "Иванович", "Николаевич", "Павлович",
            "Викторович", "Константинович", "Игоревич", "Олегович", "Романович",
            "Денисович", "Антонович", "Ильич", "Юрьевич", "Петрович", "Васильевич",
            "Георгиевич", "Кириллович", "Леонидович", "Артемович"
    };

    private static final String[] RUSSIAN_FEMALE_MIDDLE_NAMES = {
            "Александровна", "Сергеевна", "Владимировна", "Андреевна", "Алексеевна",
            "Дмитриевна", "Михайловна", "Ивановна", "Николаевна", "Павловна",
            "Викторовна", "Константиновна", "Игоревна", "Олеговна", "Романовна",
            "Денисовна", "Антоновна", "Ильинична", "Юрьевна", "Петровна", "Васильевна",
            "Георгиевна", "Кирилловна", "Леонидовна", "Артемовна"
    };

    private static final String[] RUSSIAN_LAST_NAMES = {
            "Иванов", "Смирнов", "Кузнецов", "Попов", "Васильев", "Петров", "Соколов",
            "Михайлов", "Новиков", "Федоров", "Морозов", "Волков", "Алексеев", "Лебедев",
            "Семенов", "Егоров", "Павлов", "Козлов", "Степанов", "Николаев", "Орлов",
            "Андреев", "Макаров", "Никитин", "Захаров", "Зайцев", "Соловьев", "Борисов",
            "Яковлев", "Григорьев", "Романов", "Воробьев", "Сергеев", "Кузьмин", "Фролов",
            "Александров", "Дмитриев", "Королев", "Гусев", "Киселев", "Максимов", "Поляков",
            "Сорокин", "Виноградов", "Ковалев", "Белов", "Медведев", "Антонов", "Тарасов",
            "Жуков", "Баранов", "Филиппов", "Комаров", "Давыдов", "Беляев", "Герасимов"
    };

    // Международные имена
    private static final String[] INTERNATIONAL_FIRST_NAMES = {
            "John", "James", "Robert", "Michael", "William", "David", "Richard", "Joseph",
            "Thomas", "Charles", "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth",
            "Barbara", "Susan", "Jessica", "Sarah", "Karen", "Hans", "Peter", "Klaus",
            "Wolfgang", "Dieter", "Jean", "Pierre", "Michel", "André", "Philippe"
    };

    private static final String[] INTERNATIONAL_LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis",
            "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson",
            "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson",
            "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson"
    };

    /**
     * Генерирует случайное ФИО
     */
    public PersonName generateRandomName() {
        boolean isFemale = random.nextBoolean();
        return generateRussianName(isFemale);
    }

    /**
     * Генерирует русское ФИО
     */
    private PersonName generateRussianName(boolean isFemale) {
        String firstName, middleName, lastName;

        if (isFemale) {
            firstName = RUSSIAN_FEMALE_FIRST_NAMES[random.nextInt(RUSSIAN_FEMALE_FIRST_NAMES.length)];
            middleName = RUSSIAN_FEMALE_MIDDLE_NAMES[random.nextInt(RUSSIAN_FEMALE_MIDDLE_NAMES.length)];
            lastName = RUSSIAN_LAST_NAMES[random.nextInt(RUSSIAN_LAST_NAMES.length)] + "а";
        } else {
            firstName = RUSSIAN_MALE_FIRST_NAMES[random.nextInt(RUSSIAN_MALE_FIRST_NAMES.length)];
            middleName = RUSSIAN_MALE_MIDDLE_NAMES[random.nextInt(RUSSIAN_MALE_MIDDLE_NAMES.length)];
            lastName = RUSSIAN_LAST_NAMES[random.nextInt(RUSSIAN_LAST_NAMES.length)];
        }

        return new PersonName(lastName, firstName, middleName);
    }

    /**
     * Генерирует международное имя
     */
    private PersonName generateInternationalName() {
        String firstName = INTERNATIONAL_FIRST_NAMES[random.nextInt(INTERNATIONAL_FIRST_NAMES.length)];
        String lastName = INTERNATIONAL_LAST_NAMES[random.nextInt(INTERNATIONAL_LAST_NAMES.length)];

        // Международные имена обычно без отчества
        return new PersonName(lastName, firstName, null);
    }

    /**
     * Генерирует номер телефона в зависимости от страны
     */
    public String generatePhoneNumber(Nationality nationality) {
        return switch (nationality) {
            case RUSSIA -> "+7" + generateDigits(10);
            case USA, CANADA -> "+1" + generateDigits(10);
            case UK -> "+44" + generateDigits(10);
            case GERMANY -> "+49" + generateDigits(10);
            case FRANCE -> "+33" + generateDigits(9);
            case ITALY -> "+39" + generateDigits(10);
            case SPAIN -> "+34" + generateDigits(9);
            case CHINA -> "+86" + generateDigits(11);
            case JAPAN -> "+81" + generateDigits(10);
            case KAZAKHSTAN -> "+7" + (7 + random.nextInt(3)) + generateDigits(8);
            case UKRAINE -> "+380" + generateDigits(9);
            case BELARUS -> "+375" + generateDigits(9);
            case UZBEKISTAN -> "+998" + generateDigits(9);
            default -> "+1" + generateDigits(10);
        };
    }

    /**
     * Генерирует строку из случайных цифр
     */
    private String generateDigits(int length) {
        StringBuilder sb = new StringBuilder();
        // Первая цифра не должна быть 0
        sb.append(1 + random.nextInt(9));

        for (int i = 1; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Запись для хранения ФИО
     */
    public record PersonName(String lastName, String firstName, String middleName) {}
}