package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.event.api.dto.CreateRegistrationRequest;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.api.event.RegistrationCancelledEvent;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

/**
 * Тесты для валидации, бизнес-логики и генерации кодов RegistrationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService Validation & Business Logic")
class RegistrationServiceValidationTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketTypeRepository ticketTypeRepository;

    @Mock
    private RegistrationMapper registrationMapper;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ru.aqstream.user.client.UserClient userClient;

    private RegistrationService service;

    private static final Faker FAKER = new Faker();

    private UUID tenantId;
    private UUID eventId;
    private UUID ticketTypeId;
    private UUID userId;
    private UUID registrationId;
    private String testFirstName;
    private String testLastName;
    private String testEmail;
    private String testEventTitle;
    private String testEventSlug;
    private String testTicketTypeName;

    private Event testEvent;
    private TicketType testTicketType;
    private Registration testRegistration;
    private RegistrationDto testRegistrationDto;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        service = new RegistrationService(
            registrationRepository,
            eventRepository,
            ticketTypeRepository,
            registrationMapper,
            eventPublisher,
            userClient
        );

        tenantId = UUID.randomUUID();
        eventId = UUID.randomUUID();
        ticketTypeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        registrationId = UUID.randomUUID();
        testFirstName = FAKER.name().firstName();
        testLastName = FAKER.name().lastName();
        testEmail = FAKER.internet().emailAddress();
        testEventTitle = FAKER.book().title();
        testEventSlug = "event-" + UUID.randomUUID().toString().substring(0, 8);
        testTicketTypeName = FAKER.commerce().productName();

        testEvent = createTestEvent();
        testTicketType = createTestTicketType();
        testRegistration = createTestRegistration();
        testRegistrationDto = createTestRegistrationDto();
        testPrincipal = new UserPrincipal(userId, testEmail, tenantId, Set.of("USER"));

        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private Event createTestEvent() {
        Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
        Event event = Event.create(testEventTitle, testEventSlug, startsAt, "Europe/Moscow");
        setEntityId(event, eventId);
        event.publish();
        return event;
    }

    private TicketType createTestTicketType() {
        TicketType ticketType = TicketType.create(testEvent, testTicketTypeName);
        setEntityId(ticketType, ticketTypeId);
        ticketType.updateSalesPeriod(null, null);
        ticketType.updateQuantity(100);
        return ticketType;
    }

    private Registration createTestRegistration() {
        Registration registration = Registration.create(
            testEvent,
            testTicketType,
            userId,
            "ABC12345",
            testFirstName,
            testLastName,
            testEmail
        );
        setEntityId(registration, registrationId);
        return registration;
    }

    private RegistrationDto createTestRegistrationDto() {
        return new RegistrationDto(
            registrationId,
            eventId,
            testEventTitle,
            testEventSlug,
            Instant.now().plus(7, ChronoUnit.DAYS),
            ticketTypeId,
            testTicketTypeName,
            userId,
            RegistrationStatus.CONFIRMED,
            "ABC12345",
            testFirstName,
            testLastName,
            testEmail,
            Map.of(),
            null,
            null,
            Instant.now()
        );
    }

    private void setEntityId(Object entity, UUID id) {
        try {
            var current = entity.getClass();
            while (current != null && current != Object.class) {
                try {
                    var idField = current.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(entity, id);
                    return;
                } catch (NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Nested
    @DisplayName("cancelByOrganizer()")
    class CancelByOrganizer {

        @Test
        @DisplayName("Отменяет регистрацию с указанием причины")
        void cancelByOrganizer_WithReason_CancelsWithReason() {
            // given
            String reason = "Мероприятие переносится";
            testTicketType.incrementSoldCount();

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.of(testRegistration));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(registrationRepository.save(any(Registration.class))).thenReturn(testRegistration);

            // when
            service.cancelByOrganizer(eventId, registrationId,
                new ru.aqstream.event.api.dto.CancelRegistrationRequest(reason));

            // then
            assertThat(testRegistration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
            assertThat(testRegistration.getCancellationReason()).isEqualTo(reason);

            // Проверяем публикацию события
            ArgumentCaptor<RegistrationCancelledEvent> eventCaptor =
                ArgumentCaptor.forClass(RegistrationCancelledEvent.class);
            verify(eventPublisher).publish(eventCaptor.capture());

            RegistrationCancelledEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.isCancelledByOrganizer()).isTrue();
            assertThat(publishedEvent.getCancellationReason()).isEqualTo(reason);
        }
    }

    @Nested
    @DisplayName("Registration entity бизнес-логика")
    class RegistrationBusinessLogic {

        @Test
        @DisplayName("isCancellable возвращает true для CONFIRMED")
        void isCancellable_Confirmed_ReturnsTrue() {
            assertThat(testRegistration.isCancellable()).isTrue();
        }

        @Test
        @DisplayName("isCancellable возвращает false для CANCELLED")
        void isCancellable_Cancelled_ReturnsFalse() {
            testRegistration.cancel();

            assertThat(testRegistration.isCancellable()).isFalse();
        }

        @Test
        @DisplayName("isConfirmed возвращает true для CONFIRMED")
        void isConfirmed_Confirmed_ReturnsTrue() {
            assertThat(testRegistration.isConfirmed()).isTrue();
        }

        @Test
        @DisplayName("cancel устанавливает cancelledAt")
        void cancel_SetsTimestamp() {
            assertThat(testRegistration.getCancelledAt()).isNull();

            testRegistration.cancel();

            assertThat(testRegistration.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("cancelByOrganizer устанавливает причину")
        void cancelByOrganizer_SetsReason() {
            String reason = "Тестовая причина";

            testRegistration.cancelByOrganizer(reason);

            assertThat(testRegistration.getCancellationReason()).isEqualTo(reason);
            assertThat(testRegistration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        }
    }

    @Nested
    @DisplayName("Confirmation code генерация")
    class ConfirmationCodeGeneration {

        @Test
        @DisplayName("Генерирует уникальный код при создании")
        void create_GeneratesUniqueCode() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(ticketTypeRepository.findByIdAndEventIdForUpdate(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(registrationRepository.existsByConfirmationCode(any())).thenReturn(false);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
                Registration reg = invocation.getArgument(0);
                // Проверяем формат confirmation code
                assertThat(reg.getConfirmationCode()).hasSize(8);
                assertThat(reg.getConfirmationCode()).matches("[A-Z0-9]+");
                return testRegistration;
            });
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            service.create(eventId, request, testPrincipal);

            // then
            verify(registrationRepository).existsByConfirmationCode(any());
        }
    }
}
