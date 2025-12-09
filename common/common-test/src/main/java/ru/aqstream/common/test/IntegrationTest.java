package ru.aqstream.common.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Композитная аннотация для интеграционных тестов.
 *
 * <p>Включает:</p>
 * <ul>
 *   <li>{@code @SpringBootTest} — полный Spring context</li>
 *   <li>{@code @Testcontainers} — автозапуск контейнеров</li>
 *   <li>{@code @AutoConfigureTestDatabase(replace = NONE)} — не заменять на H2</li>
 *   <li>{@code @ActiveProfiles("test")} — использовать application-test.yml</li>
 * </ul>
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @IntegrationTest}
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
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public @interface IntegrationTest {
}
