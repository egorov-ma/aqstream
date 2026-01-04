package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.NORMAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.event.api.dto.CheckInInfoDto;
import ru.aqstream.event.api.dto.CheckInResultDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.api.exception.AlreadyCheckedInException;
import ru.aqstream.event.api.exception.CheckInNotAllowedException;
import ru.aqstream.event.api.exception.RegistrationNotFoundByCodeException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.RegistrationRepository;

@UnitTest
@Feature(AllureFeatures.Features.CHECK_IN)
@DisplayName("CheckInService")
class CheckInServiceTest {

    @Mock
    private RegistrationRepository registrationRepository;

    private CheckInService checkInService;

    private static final Faker FAKER = new Faker();

    private String confirmationCode;
    private UUID tenantId;
    private UUID eventId;
    private UUID ticketTypeId;
    private UUID registrationId;
    private String eventTitle;
    private String ticketTypeName;
    private String firstName;
    private String lastName;
    private String email;

    @BeforeEach
    void setUp() {
        checkInService = new CheckInService(registrationRepository);

        // Генерируем тестовые данные
        confirmationCode = generateConfirmationCode();
        tenantId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        registrationId = UUID.randomUUID();
        eventTitle = FAKER.book().title();
        ticketTypeName = FAKER.commerce().productName();
        firstName = FAKER.name().firstName();
        lastName = FAKER.name().lastName();
        email = FAKER.internet().emailAddress();
    }

    @Nested
    @Story(AllureFeatures.Stories.CHECK_IN_PROCESS)
    @DisplayName("getCheckInInfo")
    class GetCheckInInfo {

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает информацию о регистрации")
        void getCheckInInfo_ValidCode_ReturnsInfo() {
            // Given
            Registration registration = createConfirmedRegistration();
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(registration));

            // When
            CheckInInfoDto info = checkInService.getCheckInInfo(confirmationCode);

            // Then
            assertThat(info.registrationId()).isEqualTo(registrationId);
            assertThat(info.confirmationCode()).isEqualTo(confirmationCode);
            assertThat(info.eventId()).isEqualTo(eventId);
            assertThat(info.eventTitle()).isEqualTo(eventTitle);
            assertThat(info.ticketTypeName()).isEqualTo(ticketTypeName);
            assertThat(info.firstName()).isEqualTo(firstName);
            assertThat(info.lastName()).isEqualTo(lastName);
            assertThat(info.email()).isEqualTo(email);
            assertThat(info.status()).isEqualTo(RegistrationStatus.CONFIRMED);
            assertThat(info.isCheckedIn()).isFalse();
            assertThat(info.checkedInAt()).isNull();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает информацию с отметкой о check-in")
        void getCheckInInfo_AlreadyCheckedIn_ReturnsInfoWithCheckedInAt() {
            // Given
            Registration registration = createCheckedInRegistration();
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(registration));

            // When
            CheckInInfoDto info = checkInService.getCheckInInfo(confirmationCode);

            // Then
            assertThat(info.isCheckedIn()).isTrue();
            assertThat(info.checkedInAt()).isNotNull();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если регистрация не найдена")
        void getCheckInInfo_NotFound_ThrowsException() {
            // Given
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> checkInService.getCheckInInfo(confirmationCode))
                .isInstanceOf(RegistrationNotFoundByCodeException.class);
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.CHECK_IN_PROCESS)
    @DisplayName("checkIn")
    class CheckIn {

        @Test
        @Severity(NORMAL)
        @DisplayName("успешно выполняет check-in для подтверждённой регистрации")
        void checkIn_ConfirmedRegistration_Success() {
            // Given
            Registration registration = createConfirmedRegistration();
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(registration));
            when(registrationRepository.save(any(Registration.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            // When
            CheckInResultDto result = checkInService.checkIn(confirmationCode);

            // Then
            assertThat(result.registrationId()).isEqualTo(registrationId);
            assertThat(result.confirmationCode()).isEqualTo(confirmationCode);
            assertThat(result.eventTitle()).isEqualTo(eventTitle);
            assertThat(result.ticketTypeName()).isEqualTo(ticketTypeName);
            assertThat(result.firstName()).isEqualTo(firstName);
            assertThat(result.lastName()).isEqualTo(lastName);
            assertThat(result.checkedInAt()).isNotNull();
            assertThat(result.message()).contains("успешно");

            // Проверяем что registration был сохранён
            verify(registrationRepository).save(registration);
            assertThat(registration.isCheckedIn()).isTrue();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если участник уже прошёл check-in")
        void checkIn_AlreadyCheckedIn_ThrowsException() {
            // Given
            Registration registration = createCheckedInRegistration();
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(registration));

            // When & Then
            assertThatThrownBy(() -> checkInService.checkIn(confirmationCode))
                .isInstanceOf(AlreadyCheckedInException.class);

            // Проверяем что save не вызывался
            verify(registrationRepository, never()).save(any());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение для отменённой регистрации")
        void checkIn_CancelledRegistration_ThrowsException() {
            // Given
            Registration registration = createCancelledRegistration();
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(registration));

            // When & Then
            assertThatThrownBy(() -> checkInService.checkIn(confirmationCode))
                .isInstanceOf(CheckInNotAllowedException.class);

            verify(registrationRepository, never()).save(any());
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("выбрасывает исключение если регистрация не найдена")
        void checkIn_NotFound_ThrowsException() {
            // Given
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> checkInService.checkIn(confirmationCode))
                .isInstanceOf(RegistrationNotFoundByCodeException.class);

            verify(registrationRepository, never()).save(any());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.CHECK_IN_PROCESS)
    @DisplayName("isCheckedIn")
    class IsCheckedIn {

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает false для не прошедшего check-in")
        void isCheckedIn_NotCheckedIn_ReturnsFalse() {
            // Given
            Registration registration = createConfirmedRegistration();
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(registration));

            // When
            boolean result = checkInService.isCheckedIn(confirmationCode);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @Severity(NORMAL)
        @DisplayName("возвращает true для прошедшего check-in")
        void isCheckedIn_CheckedIn_ReturnsTrue() {
            // Given
            Registration registration = createCheckedInRegistration();
            when(registrationRepository.findByConfirmationCode(confirmationCode))
                .thenReturn(Optional.of(registration));

            // When
            boolean result = checkInService.isCheckedIn(confirmationCode);

            // Then
            assertThat(result).isTrue();
        }
    }

    // === Вспомогательные методы ===

    private Registration createConfirmedRegistration() {
        Event event = createEvent();
        TicketType ticketType = createTicketType(event);
        Registration registration = Registration.create(
            event, ticketType, UUID.randomUUID(), confirmationCode, firstName, lastName, email
        );
        setPrivateField(registration, "id", registrationId);
        setPrivateField(registration, "tenantId", tenantId);
        return registration;
    }

    private Registration createCheckedInRegistration() {
        Registration registration = createConfirmedRegistration();
        registration.checkIn();
        return registration;
    }

    private Registration createCancelledRegistration() {
        Registration registration = createConfirmedRegistration();
        registration.cancel();
        return registration;
    }

    private Event createEvent() {
        Event event = Event.create(
            eventTitle,
            FAKER.internet().slug(),
            Instant.now().plus(7, ChronoUnit.DAYS),
            "Europe/Moscow"
        );
        setPrivateField(event, "id", eventId);
        setPrivateField(event, "tenantId", tenantId);
        return event;
    }

    private TicketType createTicketType(Event event) {
        TicketType ticketType = TicketType.create(event, ticketTypeName);
        ticketType.setQuantity(100);
        setPrivateField(ticketType, "id", ticketTypeId);
        return ticketType;
    }

    private String generateConfirmationCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(FAKER.random().nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Устанавливает значение private поля через reflection.
     */
    @SuppressWarnings("SameParameterValue")
    private void setPrivateField(Object object, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = findField(object.getClass(), fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Не удалось установить поле " + fieldName, e);
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Поле " + fieldName + " не найдено в иерархии классов");
    }
}
