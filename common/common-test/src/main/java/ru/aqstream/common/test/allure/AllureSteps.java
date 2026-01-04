package ru.aqstream.common.test.allure;

import io.qameta.allure.Step;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Утилитные Allure steps для тестов.
 *
 * <p>Предоставляет готовые @Step методы для обёртывания тестовых операций.
 * Все методы принимают Supplier и выполняют его внутри Allure step.</p>
 *
 * <p>Использование:</p>
 * <pre>
 * User user = AllureSteps.createTestUser(email, () -&gt;
 *     User.createWithEmail(email, hash, firstName, lastName)
 * );
 *
 * AuthResponse response = AllureSteps.callService("AuthService", "register", () -&gt;
 *     authService.register(request, userAgent, ip)
 * );
 * </pre>
 */
public final class AllureSteps {

    /**
     * Создание тестового пользователя.
     *
     * @param email email пользователя (для отображения в step)
     * @param supplier функция создания пользователя
     * @return созданный объект
     */
    @Step("Создать тестового пользователя: email={email}")
    public static <T> T createTestUser(String email, Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Создание тестового события.
     *
     * @param title название события (для отображения в step)
     * @param supplier функция создания события
     * @return созданный объект
     */
    @Step("Создать тестовое событие: title={title}")
    public static <T> T createTestEvent(String title, Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Создание тестовой организации.
     *
     * @param name название организации (для отображения в step)
     * @param supplier функция создания организации
     * @return созданный объект
     */
    @Step("Создать тестовую организацию: name={name}")
    public static <T> T createTestOrganization(String name, Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Вызов метода сервиса.
     *
     * @param serviceName название сервиса
     * @param methodName название метода
     * @param supplier функция вызова метода
     * @return результат вызова
     */
    @Step("Вызвать {serviceName}.{methodName}()")
    public static <T> T callService(String serviceName, String methodName, Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Выполнение HTTP запроса.
     *
     * @param httpMethod HTTP метод (GET, POST, PUT, DELETE)
     * @param url URL запроса
     * @param supplier функция выполнения запроса
     * @return результат запроса
     */
    @Step("Выполнить {httpMethod} запрос: {url}")
    public static <T> T performRequest(String httpMethod, String url, Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Проверка ответа API.
     *
     * @param expectedStatus ожидаемый HTTP статус
     * @param assertion функция с assertions
     */
    @Step("Проверить ответ: statusCode={expectedStatus}")
    public static void verifyResponse(int expectedStatus, Runnable assertion) {
        assertion.run();
    }

    /**
     * Проверка данных в БД.
     *
     * @param entityType тип сущности
     * @param entityId ID сущности
     * @param assertion функция с assertions
     */
    @Step("Проверить данные в БД: entity={entityType}, id={entityId}")
    public static void verifyDatabase(String entityType, UUID entityId, Runnable assertion) {
        assertion.run();
    }

    /**
     * Настройка мока.
     *
     * @param mockName название мока
     * @param setup функция настройки
     */
    @Step("Настроить мок: {mockName}")
    public static void setupMock(String mockName, Runnable setup) {
        setup.run();
    }

    /**
     * Выполнение действия с описанием.
     *
     * @param description описание действия
     * @param action функция действия
     */
    @Step("{description}")
    public static void step(String description, Runnable action) {
        action.run();
    }

    /**
     * Выполнение действия с описанием и возвратом значения.
     *
     * @param description описание действия
     * @param supplier функция действия
     * @return результат действия
     */
    @Step("{description}")
    public static <T> T step(String description, Supplier<T> supplier) {
        return supplier.get();
    }

    private AllureSteps() {
        // Утилитный класс
    }
}
