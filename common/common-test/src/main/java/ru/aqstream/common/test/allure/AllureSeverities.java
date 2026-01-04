package ru.aqstream.common.test.allure;

import io.qameta.allure.SeverityLevel;

/**
 * Хелперы для определения severity тестов.
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @Test}
 * {@code @Severity(AllureSeverities.blocker())}
 * void criticalTest() {
 *     // ...
 * }
 * </pre>
 */
public final class AllureSeverities {

    /**
     * BLOCKER - критичные функции (auth, создание событий, RLS).
     * Падение этих тестов блокирует релиз.
     */
    public static SeverityLevel blocker() {
        return SeverityLevel.BLOCKER;
    }

    /**
     * CRITICAL - важные функции (регистрации, публикация, платежи).
     * Падение этих тестов требует немедленного внимания.
     */
    public static SeverityLevel critical() {
        return SeverityLevel.CRITICAL;
    }

    /**
     * NORMAL - обычные функции (валидации, обновления, уведомления).
     */
    public static SeverityLevel normal() {
        return SeverityLevel.NORMAL;
    }

    /**
     * MINOR - второстепенные функции (форматирование, служебные операции).
     */
    public static SeverityLevel minor() {
        return SeverityLevel.MINOR;
    }

    /**
     * TRIVIAL - тривиальные функции.
     */
    public static SeverityLevel trivial() {
        return SeverityLevel.TRIVIAL;
    }

    private AllureSeverities() {
        // Утилитный класс
    }
}
