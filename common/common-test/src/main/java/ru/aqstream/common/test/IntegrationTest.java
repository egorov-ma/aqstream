package ru.aqstream.common.test;

import io.qameta.allure.Epic;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.aqstream.common.test.allure.AllureFeatures;

/**
 * Композитная аннотация для интеграционных тестов.
 *
 * <p>Включает:</p>
 * <ul>
 *   <li>{@code @Tag("integration")} — тег для фильтрации тестов</li>
 *   <li>{@code @SpringBootTest} — полный Spring context</li>
 *   <li>{@code @Testcontainers} — автозапуск контейнеров</li>
 *   <li>{@code @AutoConfigureTestDatabase(replace = NONE)} — не заменять на H2</li>
 *   <li>{@code @ActiveProfiles("test")} — использовать application-test.yml</li>
 *   <li>{@code @Epic("Integration Tests")} — группировка в Allure отчётах</li>
 * </ul>
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @IntegrationTest}
 * {@code @Feature(AllureFeatures.Features.EVENT_MANAGEMENT)}
 * class EventServiceIntegrationTest extends PostgresTestContainer {
 *
 *     @Autowired
 *     private EventService eventService;
 *
 *     @Test
 *     void create_ValidRequest_SavesEvent() {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>Запуск тестов:</p>
 * <ul>
 *   <li>{@code ./gradlew test} — все тесты (unit + integration + e2e)</li>
 *   <li>{@code ./gradlew unit} — только unit тесты</li>
 *   <li>{@code ./gradlew integration} — только интеграционные тесты</li>
 *   <li>{@code ./gradlew e2e} — только E2E тесты</li>
 * </ul>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Tag("integration")
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Epic(AllureFeatures.Epics.INTEGRATION_TESTS)
public @interface IntegrationTest {
}
