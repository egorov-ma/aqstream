package ru.aqstream.common.test;

import io.qameta.allure.Epic;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import ru.aqstream.common.test.allure.AllureFeatures;

/**
 * Композитная аннотация для E2E тестов (Java-based).
 *
 * <p>Включает:</p>
 * <ul>
 *   <li>{@code @Tag("e2e")} — тег для фильтрации тестов</li>
 *   <li>{@code @Epic("E2E Tests")} — группировка в Allure отчётах</li>
 * </ul>
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @E2ETest}
 * {@code @Feature(AllureFeatures.Features.EVENT_MANAGEMENT)}
 * {@code @DisplayName("Event Registration Flow")}
 * class EventRegistrationE2ETest {
 *     // ...
 * }
 * </pre>
 *
 * <p>Примечание: Основные E2E тесты находятся в frontend (Playwright).
 * Эта аннотация используется для Java-based E2E тестов если они есть.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("e2e")
@Epic(AllureFeatures.Epics.E2E_TESTS)
public @interface E2ETest {
}
