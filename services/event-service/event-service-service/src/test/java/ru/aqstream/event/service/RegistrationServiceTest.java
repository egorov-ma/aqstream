package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import ru.aqstream.event.api.event.RegistrationCreatedEvent;
import ru.aqstream.event.api.exception.EventNotFoundException;
import ru.aqstream.event.api.exception.EventRegistrationClosedException;
import ru.aqstream.event.api.exception.RegistrationAccessDeniedException;
import ru.aqstream.event.api.exception.RegistrationAlreadyExistsException;
import ru.aqstream.event.api.exception.RegistrationNotCancellableException;
import ru.aqstream.event.api.exception.RegistrationNotFoundException;
import ru.aqstream.event.api.exception.TicketTypeNotFoundException;
import ru.aqstream.event.api.exception.TicketTypeSalesNotOpenException;
import ru.aqstream.event.api.exception.TicketTypeSoldOutException;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.db.repository.TicketTypeRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegistrationService")
class RegistrationServiceTest {

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

        // Устанавливаем tenant context
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
        // Публикуем событие
        event.publish();
        return event;
    }

    private TicketType createTestTicketType() {
        TicketType ticketType = TicketType.create(testEvent, testTicketTypeName);
        setEntityId(ticketType, ticketTypeId);
        // Без ограничений по периоду продаж
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
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Создаёт регистрацию на событие")
        void create_ValidRequest_ReturnsRegistration() {
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
            when(registrationRepository.save(any(Registration.class))).thenReturn(testRegistration);
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            RegistrationDto result = service.create(eventId, request, testPrincipal);

            // then
            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(RegistrationStatus.CONFIRMED);

            // Проверяем, что sold_count увеличен
            verify(ticketTypeRepository).save(any(TicketType.class));

            // Проверяем публикацию события
            verify(eventPublisher).publish(any(RegistrationCreatedEvent.class));
        }

        @Test
        @DisplayName("Увеличивает soldCount при создании регистрации")
        void create_ValidRequest_IncrementsSoldCount() {
            // given
            int initialSoldCount = testTicketType.getSoldCount();
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(ticketTypeRepository.findByIdAndEventIdForUpdate(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(registrationRepository.existsByConfirmationCode(any())).thenReturn(false);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(registrationRepository.save(any(Registration.class))).thenReturn(testRegistration);
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            service.create(eventId, request, testPrincipal);

            // then
            assertThat(testTicketType.getSoldCount()).isEqualTo(initialSoldCount + 1);
        }

        @Test
        @DisplayName("Выбрасывает EventNotFoundException если событие не найдено")
        void create_EventNotFound_ThrowsException() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request, testPrincipal))
                .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("Выбрасывает EventRegistrationClosedException если событие не опубликовано")
        void create_EventNotPublished_ThrowsException() {
            // given
            Event draftEvent = Event.create("Draft Event", "draft-event",
                Instant.now().plus(7, ChronoUnit.DAYS), "Europe/Moscow");
            setEntityId(draftEvent, eventId);
            // Событие остаётся в статусе DRAFT

            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(draftEvent));

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request, testPrincipal))
                .isInstanceOf(EventRegistrationClosedException.class);
        }

        @Test
        @DisplayName("Выбрасывает RegistrationAlreadyExistsException если пользователь уже зарегистрирован")
        void create_AlreadyRegistered_ThrowsException() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(true);

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request, testPrincipal))
                .isInstanceOf(RegistrationAlreadyExistsException.class);

            // Проверяем, что регистрация не создана
            verify(registrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Выбрасывает TicketTypeNotFoundException если тип билета не найден")
        void create_TicketTypeNotFound_ThrowsException() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(ticketTypeRepository.findByIdAndEventIdForUpdate(ticketTypeId, eventId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request, testPrincipal))
                .isInstanceOf(TicketTypeNotFoundException.class);
        }

        @Test
        @DisplayName("Выбрасывает TicketTypeSoldOutException если билеты распроданы")
        void create_SoldOut_ThrowsException() {
            // given
            testTicketType.updateQuantity(1);
            testTicketType.incrementSoldCount(); // Теперь билеты распроданы

            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(ticketTypeRepository.findByIdAndEventIdForUpdate(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request, testPrincipal))
                .isInstanceOf(TicketTypeSoldOutException.class);
        }

        @Test
        @DisplayName("Выбрасывает TicketTypeSalesNotOpenException если продажи закрыты")
        void create_SalesNotOpen_ThrowsException() {
            // given
            testTicketType.updateSalesPeriod(
                Instant.now().plus(1, ChronoUnit.DAYS),  // Продажи начнутся завтра
                Instant.now().plus(7, ChronoUnit.DAYS)
            );

            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, testFirstName, testLastName, testEmail, null
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(ticketTypeRepository.findByIdAndEventIdForUpdate(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));

            // when/then
            assertThatThrownBy(() -> service.create(eventId, request, testPrincipal))
                .isInstanceOf(TicketTypeSalesNotOpenException.class);
        }
    }

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("Возвращает регистрацию по ID для владельца")
        void getById_Owner_ReturnsRegistration() {
            // given
            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.of(testRegistration));
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            RegistrationDto result = service.getById(registrationId, testPrincipal);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(registrationId);
        }

        @Test
        @DisplayName("Выбрасывает RegistrationNotFoundException если регистрация не найдена")
        void getById_NotFound_ThrowsException() {
            // given
            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.getById(registrationId, testPrincipal))
                .isInstanceOf(RegistrationNotFoundException.class);
        }

        @Test
        @DisplayName("Выбрасывает RegistrationAccessDeniedException для чужой регистрации")
        void getById_NotOwner_ThrowsException() {
            // given
            UUID otherUserId = UUID.randomUUID();
            UserPrincipal otherPrincipal = new UserPrincipal(otherUserId, "other@test.com", tenantId, Set.of("USER"));

            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.of(testRegistration));

            // when/then
            assertThatThrownBy(() -> service.getById(registrationId, otherPrincipal))
                .isInstanceOf(RegistrationAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("Отменяет регистрацию и уменьшает soldCount")
        void cancel_ValidRequest_CancelsAndDecrementsCount() {
            // given
            testTicketType.incrementSoldCount(); // Была продажа
            int initialSoldCount = testTicketType.getSoldCount();

            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.of(testRegistration));
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(registrationRepository.save(any(Registration.class))).thenReturn(testRegistration);

            // when
            service.cancel(registrationId, testPrincipal);

            // then
            assertThat(testRegistration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
            assertThat(testRegistration.getCancelledAt()).isNotNull();
            assertThat(testTicketType.getSoldCount()).isEqualTo(initialSoldCount - 1);

            // Проверяем публикацию события
            verify(eventPublisher).publish(any(RegistrationCancelledEvent.class));
        }

        @Test
        @DisplayName("Выбрасывает RegistrationAccessDeniedException для чужой регистрации")
        void cancel_NotOwner_ThrowsException() {
            // given
            UUID otherUserId = UUID.randomUUID();
            UserPrincipal otherPrincipal = new UserPrincipal(otherUserId, "other@test.com", tenantId, Set.of("USER"));

            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.of(testRegistration));

            // when/then
            assertThatThrownBy(() -> service.cancel(registrationId, otherPrincipal))
                .isInstanceOf(RegistrationAccessDeniedException.class);

            // Регистрация не отменена
            assertThat(testRegistration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("Выбрасывает RegistrationNotCancellableException для уже отменённой регистрации")
        void cancel_AlreadyCancelled_ThrowsException() {
            // given
            testRegistration.cancel(); // Уже отменена

            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.of(testRegistration));

            // when/then
            assertThatThrownBy(() -> service.cancel(registrationId, testPrincipal))
                .isInstanceOf(RegistrationNotCancellableException.class);
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
            // when/then
            assertThat(testRegistration.isCancellable()).isTrue();
        }

        @Test
        @DisplayName("isCancellable возвращает false для CANCELLED")
        void isCancellable_Cancelled_ReturnsFalse() {
            // given
            testRegistration.cancel();

            // when/then
            assertThat(testRegistration.isCancellable()).isFalse();
        }

        @Test
        @DisplayName("isConfirmed возвращает true для CONFIRMED")
        void isConfirmed_Confirmed_ReturnsTrue() {
            // when/then
            assertThat(testRegistration.isConfirmed()).isTrue();
        }

        @Test
        @DisplayName("cancel устанавливает cancelledAt")
        void cancel_SetsTimestamp() {
            // given
            assertThat(testRegistration.getCancelledAt()).isNull();

            // when
            testRegistration.cancel();

            // then
            assertThat(testRegistration.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("cancelByOrganizer устанавливает причину")
        void cancelByOrganizer_SetsReason() {
            // given
            String reason = "Тестовая причина";

            // when
            testRegistration.cancelByOrganizer(reason);

            // then
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
