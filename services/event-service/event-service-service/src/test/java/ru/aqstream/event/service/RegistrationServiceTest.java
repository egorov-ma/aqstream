package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static io.qameta.allure.SeverityLevel.CRITICAL;

import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;

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
import org.mockito.Mock;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.event.api.dto.CreateRegistrationRequest;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
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

@UnitTest
@Feature(AllureFeatures.Features.REGISTRATIONS)
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
    private RegistrationEventPublisher registrationEventPublisher;

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
            registrationEventPublisher,
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
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("create()")
    class Create {

        @Test
        @Severity(CRITICAL)
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
            verify(registrationEventPublisher).publishCreated(any(Registration.class));
        }

        @Test
        @Severity(CRITICAL)
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
        @Severity(CRITICAL)
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
        @Severity(CRITICAL)
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
        @Severity(CRITICAL)
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
        @Severity(CRITICAL)
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
        @Severity(CRITICAL)
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
        @Severity(CRITICAL)
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
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("Автозаполнение данных из профиля")
    class AutofillFromProfile {

        @Test
        @Severity(CRITICAL)
        @DisplayName("createForPublicEvent: без личных данных - получает из UserService")
        void createForPublicEvent_NoPersonalInfo_FetchesFromUserService() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, null, null, null, null
            );

            ru.aqstream.user.api.dto.UserDto userDto = new ru.aqstream.user.api.dto.UserDto(
                userId, testEmail, testFirstName, testLastName,
                null, true, false, Instant.now()
            );

            when(eventRepository.findPublicBySlug(testEventSlug)).thenReturn(Optional.of(testEvent));
            when(userClient.findById(userId)).thenReturn(Optional.of(userDto));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(ticketTypeRepository.findByIdAndEventIdForUpdate(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(registrationRepository.existsByConfirmationCode(any())).thenReturn(false);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(registrationRepository.save(any(Registration.class))).thenReturn(testRegistration);
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            RegistrationDto result = service.createForPublicEvent(testEventSlug, request, testPrincipal);

            // then
            assertThat(result).isNotNull();
            verify(userClient).findById(userId);  // Проверяем вызов UserClient
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("createForPublicEvent: с личными данными - использует данные из request")
        void createForPublicEvent_WithPersonalInfo_UsesRequestData() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, "CustomName", "CustomLast", "custom@example.com", null
            );

            when(eventRepository.findPublicBySlug(testEventSlug)).thenReturn(Optional.of(testEvent));
            when(registrationRepository.existsActiveByEventIdAndUserId(eventId, userId)).thenReturn(false);
            when(ticketTypeRepository.findByIdAndEventIdForUpdate(ticketTypeId, eventId))
                .thenReturn(Optional.of(testTicketType));
            when(registrationRepository.existsByConfirmationCode(any())).thenReturn(false);
            when(ticketTypeRepository.save(any(TicketType.class))).thenReturn(testTicketType);
            when(registrationRepository.save(any(Registration.class))).thenReturn(testRegistration);
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            RegistrationDto result = service.createForPublicEvent(testEventSlug, request, testPrincipal);

            // then
            assertThat(result).isNotNull();
            verify(userClient, never()).findById(any());  // НЕ вызывается
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("createForPublicEvent: пользователь не найден - выбрасывает UserNotFoundException")
        void createForPublicEvent_UserNotFound_ThrowsUserNotFoundException() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, null, null, null, null
            );

            when(userClient.findById(userId)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() ->
                service.createForPublicEvent(testEventSlug, request, testPrincipal))
                .isInstanceOf(ru.aqstream.common.api.exception.UserNotFoundException.class);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("create: без личных данных - получает из UserService")
        void create_NoPersonalInfo_FetchesFromUserService() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, null, null, null, null
            );

            ru.aqstream.user.api.dto.UserDto userDto = new ru.aqstream.user.api.dto.UserDto(
                userId, testEmail, testFirstName, testLastName,
                null, true, false, Instant.now()
            );

            when(eventRepository.findByIdAndTenantId(eventId, tenantId)).thenReturn(Optional.of(testEvent));
            when(userClient.findById(userId)).thenReturn(Optional.of(userDto));
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
            verify(userClient).findById(userId);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("create: с личными данными - использует данные из request")
        void create_WithPersonalInfo_UsesRequestData() {
            // given
            CreateRegistrationRequest request = new CreateRegistrationRequest(
                ticketTypeId, "CustomName", "CustomLast", "custom@example.com", null
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
            verify(userClient, never()).findById(any());
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("getById()")
    class GetById {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Возвращает регистрацию по ID для владельца")
        void getById_Owner_ReturnsRegistration() {
            // given
            when(registrationRepository.findByIdAndUserId(registrationId, userId))
                .thenReturn(Optional.of(testRegistration));
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            RegistrationDto result = service.getById(registrationId, testPrincipal);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(registrationId);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает RegistrationNotFoundException если регистрация не найдена")
        void getById_NotFound_ThrowsException() {
            // given
            when(registrationRepository.findByIdAndUserId(registrationId, userId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.getById(registrationId, testPrincipal))
                .isInstanceOf(RegistrationNotFoundException.class);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Возвращает регистрацию организатору из своего tenant")
        void getById_Organizer_ReturnsRegistration() {
            // given
            UUID organizerId = UUID.randomUUID();
            UserPrincipal organizerPrincipal = new UserPrincipal(
                organizerId, "organizer@test.com", tenantId, Set.of("USER", "ORGANIZER")
            );

            // Организатор не владелец регистрации
            when(registrationRepository.findByIdAndUserId(registrationId, organizerId))
                .thenReturn(Optional.empty());
            // Но может найти по tenant
            when(registrationRepository.findByIdAndTenantId(registrationId, tenantId))
                .thenReturn(Optional.of(testRegistration));
            when(registrationMapper.toDto(testRegistration)).thenReturn(testRegistrationDto);

            // when
            RegistrationDto result = service.getById(registrationId, organizerPrincipal);

            // then
            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(registrationId);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает RegistrationNotFoundException для не-владельца и не-организатора")
        void getById_NotOwnerNotOrganizer_ThrowsException() {
            // given
            UUID otherUserId = UUID.randomUUID();
            UserPrincipal otherPrincipal = new UserPrincipal(otherUserId, "other@test.com", tenantId, Set.of("USER"));

            when(registrationRepository.findByIdAndUserId(registrationId, otherUserId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.getById(registrationId, otherPrincipal))
                .isInstanceOf(RegistrationNotFoundException.class);
        }
    }

    @Nested
    @Story(AllureFeatures.Stories.REGISTRATION_FLOW)
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @Severity(CRITICAL)
        @DisplayName("Отменяет регистрацию и уменьшает soldCount")
        void cancel_ValidRequest_CancelsAndDecrementsCount() {
            // given
            testTicketType.incrementSoldCount(); // Была продажа
            int initialSoldCount = testTicketType.getSoldCount();

            when(registrationRepository.findByIdAndUserId(registrationId, userId))
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
            verify(registrationEventPublisher).publishCancelled(any(Registration.class), eq(false));
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает RegistrationNotFoundException для чужой регистрации")
        void cancel_NotOwner_ThrowsException() {
            // given
            UUID otherUserId = UUID.randomUUID();
            UserPrincipal otherPrincipal = new UserPrincipal(otherUserId, "other@test.com", tenantId, Set.of("USER"));

            // Регистрация не найдена по userId другого пользователя
            when(registrationRepository.findByIdAndUserId(registrationId, otherUserId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.cancel(registrationId, otherPrincipal))
                .isInstanceOf(RegistrationNotFoundException.class);

            // Регистрация не отменена
            assertThat(testRegistration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
        }

        @Test
        @Severity(CRITICAL)
        @DisplayName("Выбрасывает RegistrationNotCancellableException для уже отменённой регистрации")
        void cancel_AlreadyCancelled_ThrowsException() {
            // given
            testRegistration.cancel(); // Уже отменена

            when(registrationRepository.findByIdAndUserId(registrationId, userId))
                .thenReturn(Optional.of(testRegistration));

            // when/then
            assertThatThrownBy(() -> service.cancel(registrationId, testPrincipal))
                .isInstanceOf(RegistrationNotCancellableException.class);
        }
    }

}
