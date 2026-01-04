package ru.aqstream.common.test;

import io.qameta.allure.Epic;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.common.test.allure.AllureFeatures;

/**
 * Композитная аннотация для unit тестов.
 *
 * <p>Включает:</p>
 * <ul>
 *   <li>{@code @ExtendWith(MockitoExtension.class)} — Mockito для моков</li>
 *   <li>{@code @Epic("Unit Tests")} — группировка в Allure отчётах</li>
 * </ul>
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @UnitTest}
 * {@code @Feature(AllureFeatures.Features.USER_MANAGEMENT)}
 * {@code @DisplayName("AuthService")}
 * class AuthServiceTest {
 *
 *     {@code @Nested}
 *     {@code @Story(AllureFeatures.Stories.AUTHENTICATION)}
 *     {@code @DisplayName("register")}
 *     class Register {
 *
 *         {@code @Test}
 *         {@code @Severity(BLOCKER)}
 *         {@code @DisplayName("успешно регистрирует нового пользователя")}
 *         void register_ValidRequest_ReturnsAuthResponse() {
 *             // ...
 *         }
 *     }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@ExtendWith(MockitoExtension.class)
@Epic(AllureFeatures.Epics.UNIT_TESTS)
public @interface UnitTest {
}
