package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.model.User;
import feign.FeignException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.client.EventClient;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.OrganizationRole;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.client.UserClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeeplinkHandler")
class DeeplinkHandlerTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private TelegramMessageSender messageSender;

    @Mock
    private UserClient userClient;

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private DeeplinkHandler deeplinkHandler;

    private Long chatId;
    private User telegramUser;
    private Long telegramId;

    @BeforeEach
    void setUp() {
        chatId = FAKER.number().numberBetween(100000000L, 999999999L);
        telegramId = FAKER.number().numberBetween(100000000L, 999999999L);
        telegramUser = mock(User.class);
        lenient().when(telegramUser.id()).thenReturn(telegramId);
    }

    // ==================== handleAuth ====================

    @Nested
    @DisplayName("handleAuth")
    class HandleAuth {

        private String authToken;

        @BeforeEach
        void setUp() {
            authToken = FAKER.regexify("[a-zA-Z0-9]{32}");
        }

        @Test
        @DisplayName("from is null — отправляет сообщение об ошибке")
        void handleAuth_FromNull_SendsErrorMessage() {
            // when
            deeplinkHandler.handleAuth(chatId, authToken, null);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Не удалось получить информацию"));
            verify(messageSender, never()).sendMessageWithButtons(any(), any(), any());
        }

        @Test
        @DisplayName("валидный запрос — отправляет сообщение с кнопкой подтверждения")
        void handleAuth_ValidRequest_SendsConfirmButton() {
            // when
            deeplinkHandler.handleAuth(chatId, authToken, telegramUser);

            // then
            verify(messageSender).sendMessageWithButtons(
                eq(chatId),
                contains("Вход в AqStream"),
                any(String[][].class)
            );
        }

        @Test
        @DisplayName("сообщение содержит инструкцию для подтверждения")
        void handleAuth_MessageContainsConfirmInstructions() {
            // when
            deeplinkHandler.handleAuth(chatId, authToken, telegramUser);

            // then
            verify(messageSender).sendMessageWithButtons(
                eq(chatId),
                contains("нажмите кнопку"),
                any(String[][].class)
            );
        }
    }

    // ==================== handleInvite ====================

    @Nested
    @DisplayName("handleInvite")
    class HandleInvite {

        private String inviteCode;

        @BeforeEach
        void setUp() {
            inviteCode = FAKER.regexify("[a-zA-Z0-9]{8}");
        }

        @Test
        @DisplayName("from is null — отправляет сообщение о необходимости регистрации")
        void handleInvite_FromNull_SendsNotRegisteredMessage() {
            // when
            deeplinkHandler.handleInvite(chatId, inviteCode, null);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Чтобы принять приглашение"));
            verify(userClient, never()).findByTelegramId(any());
        }

        @Test
        @DisplayName("пользователь не найден по telegramId — отправляет сообщение о необходимости регистрации")
        void handleInvite_UserNotFound_SendsNotRegisteredMessage() {
            // given
            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.empty());

            // when
            deeplinkHandler.handleInvite(chatId, inviteCode, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Чтобы принять приглашение"));
        }

        @Test
        @DisplayName("приглашение успешно принято — отправляет сообщение об успехе")
        void handleInvite_Success_SendsSuccessMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UserDto user = createUserDto(userId);
            OrganizationMemberDto member = OrganizationMemberDto.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .role(OrganizationRole.MODERATOR)
                .joinedAt(Instant.now())
                .build();

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(userClient.acceptInviteByTelegram(any())).thenReturn(member);

            // when
            deeplinkHandler.handleInvite(chatId, inviteCode, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Добро пожаловать"));
            verify(messageSender).sendMessage(eq(chatId), contains("Модератор"));
        }

        @Test
        @DisplayName("приглашение не найдено — отправляет сообщение об ошибке")
        void handleInvite_InviteNotFound_SendsErrorMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UserDto user = createUserDto(userId);

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(userClient.acceptInviteByTelegram(any())).thenThrow(FeignException.NotFound.class);

            // when
            deeplinkHandler.handleInvite(chatId, inviteCode, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("не найдено"));
        }

        @Test
        @DisplayName("конфликт (уже член организации) — отправляет сообщение об ошибке")
        void handleInvite_AlreadyMember_SendsErrorMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UserDto user = createUserDto(userId);

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(userClient.acceptInviteByTelegram(any())).thenThrow(FeignException.Conflict.class);

            // when
            deeplinkHandler.handleInvite(chatId, inviteCode, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Не удалось принять приглашение"));
        }

        @Test
        @DisplayName("ошибка поиска пользователя — отправляет сообщение об ошибке")
        void handleInvite_UserLookupError_SendsErrorMessage() {
            // given
            when(userClient.findByTelegramId(any())).thenThrow(FeignException.class);

            // when
            deeplinkHandler.handleInvite(chatId, inviteCode, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Произошла ошибка"));
        }
    }

    // ==================== handleLink ====================

    @Nested
    @DisplayName("handleLink")
    class HandleLink {

        private String linkToken;

        @BeforeEach
        void setUp() {
            linkToken = FAKER.regexify("[a-zA-Z0-9]{32}");
        }

        @Test
        @DisplayName("from is null — отправляет сообщение об ошибке")
        void handleLink_FromNull_SendsErrorMessage() {
            // when
            deeplinkHandler.handleLink(chatId, linkToken, null);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Не удалось получить информацию"));
            verify(userClient, never()).linkTelegramByToken(any());
        }

        @Test
        @DisplayName("привязка успешна — отправляет сообщение об успехе")
        void handleLink_Success_SendsSuccessMessage() {
            // when
            deeplinkHandler.handleLink(chatId, linkToken, telegramUser);

            // then
            verify(userClient).linkTelegramByToken(any());
            verify(messageSender).sendMessage(eq(chatId), contains("Telegram привязан"));
        }

        @Test
        @DisplayName("токен не найден — отправляет сообщение о недействительном токене")
        void handleLink_TokenNotFound_SendsNotFoundMessage() {
            // given
            FeignException.NotFound notFound = mock(FeignException.NotFound.class);
            doThrow(notFound).when(userClient).linkTelegramByToken(any());

            // when
            deeplinkHandler.handleLink(chatId, linkToken, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Токен недействителен"));
        }

        @Test
        @DisplayName("Telegram уже привязан к другому аккаунту — отправляет сообщение о конфликте")
        void handleLink_TelegramAlreadyLinked_SendsConflictMessage() {
            // given
            FeignException.Conflict conflict = mock(FeignException.Conflict.class);
            doThrow(conflict).when(userClient).linkTelegramByToken(any());

            // when
            deeplinkHandler.handleLink(chatId, linkToken, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("уже привязан к другому пользователю"));
        }

        @Test
        @DisplayName("общая ошибка — отправляет сообщение об ошибке")
        void handleLink_GeneralError_SendsErrorMessage() {
            // given
            doThrow(FeignException.class).when(userClient).linkTelegramByToken(any());

            // when
            deeplinkHandler.handleLink(chatId, linkToken, telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Произошла ошибка"));
        }
    }

    // ==================== handleRegistration ====================

    @Nested
    @DisplayName("handleRegistration")
    class HandleRegistration {

        private UUID registrationId;

        @BeforeEach
        void setUp() {
            registrationId = UUID.randomUUID();
        }

        @Test
        @DisplayName("from is null — отправляет сообщение о необходимости авторизации")
        void handleRegistration_FromNull_SendsAuthRequiredMessage() {
            // when
            deeplinkHandler.handleRegistration(chatId, registrationId.toString(), null);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Для просмотра билета"));
            verify(userClient, never()).findByTelegramId(any());
        }

        @Test
        @DisplayName("невалидный UUID — отправляет сообщение об ошибке")
        void handleRegistration_InvalidUuid_SendsErrorMessage() {
            // when
            deeplinkHandler.handleRegistration(chatId, "invalid-uuid", telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Неверный формат"));
            verify(userClient, never()).findByTelegramId(any());
        }

        @Test
        @DisplayName("пользователь не найден по telegramId — отправляет сообщение о необходимости авторизации")
        void handleRegistration_UserNotFound_SendsAuthRequiredMessage() {
            // given
            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.empty());

            // when
            deeplinkHandler.handleRegistration(chatId, registrationId.toString(), telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Для просмотра билета"));
        }

        @Test
        @DisplayName("регистрация не найдена — отправляет сообщение об ошибке")
        void handleRegistration_RegistrationNotFound_SendsErrorMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UserDto user = createUserDto(userId);

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(eventClient.findRegistrationById(registrationId)).thenReturn(Optional.empty());

            // when
            deeplinkHandler.handleRegistration(chatId, registrationId.toString(), telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Регистрация не найдена"));
        }

        @Test
        @DisplayName("регистрация принадлежит другому пользователю — отправляет сообщение об ошибке")
        void handleRegistration_DifferentUser_SendsAccessDeniedMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UUID anotherUserId = UUID.randomUUID();
            UserDto user = createUserDto(userId);
            RegistrationDto registration = createRegistrationDto(
                registrationId, anotherUserId, RegistrationStatus.CONFIRMED);

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(eventClient.findRegistrationById(registrationId)).thenReturn(Optional.of(registration));

            // when
            deeplinkHandler.handleRegistration(chatId, registrationId.toString(), telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("нет доступа"));
        }

        @Test
        @DisplayName("регистрация отменена — отправляет сообщение об отмене")
        void handleRegistration_Cancelled_SendsCancelledMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UserDto user = createUserDto(userId);
            RegistrationDto registration = createRegistrationDto(registrationId, userId, RegistrationStatus.CANCELLED);

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(eventClient.findRegistrationById(registrationId)).thenReturn(Optional.of(registration));

            // when
            deeplinkHandler.handleRegistration(chatId, registrationId.toString(), telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Регистрация отменена"));
        }

        @Test
        @DisplayName("регистрация подтверждена — отправляет информацию о билете")
        void handleRegistration_Confirmed_SendsTicketMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UserDto user = createUserDto(userId);
            RegistrationDto registration = createRegistrationDto(registrationId, userId, RegistrationStatus.CONFIRMED);

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(eventClient.findRegistrationById(registrationId)).thenReturn(Optional.of(registration));

            // when
            deeplinkHandler.handleRegistration(chatId, registrationId.toString(), telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Ваш билет"));
            verify(messageSender).sendMessage(eq(chatId), contains("Подтверждена"));
        }

        @Test
        @DisplayName("ошибка при получении регистрации — отправляет сообщение об ошибке")
        void handleRegistration_EventClientError_SendsErrorMessage() {
            // given
            UUID userId = UUID.randomUUID();
            UserDto user = createUserDto(userId);

            when(userClient.findByTelegramId(String.valueOf(telegramId))).thenReturn(Optional.of(user));
            when(eventClient.findRegistrationById(registrationId)).thenThrow(FeignException.class);

            // when
            deeplinkHandler.handleRegistration(chatId, registrationId.toString(), telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Произошла ошибка"));
        }
    }

    // ==================== Helper Methods ====================

    private UserDto createUserDto(UUID userId) {
        return new UserDto(
            userId,
            FAKER.internet().emailAddress(),
            FAKER.name().firstName(),
            FAKER.name().lastName(),
            null, // avatarUrl
            true, // emailVerified
            false, // isAdmin
            Instant.now()
        );
    }

    private RegistrationDto createRegistrationDto(UUID regId, UUID userId, RegistrationStatus status) {
        return new RegistrationDto(
            regId,
            UUID.randomUUID(), // eventId
            FAKER.book().title(), // eventTitle
            FAKER.internet().slug(), // eventSlug
            Instant.now().plusSeconds(86400), // eventStartsAt
            UUID.randomUUID(), // ticketTypeId
            "Стандартный", // ticketTypeName
            userId,
            status,
            FAKER.regexify("[A-Z0-9]{8}"), // confirmationCode
            FAKER.name().firstName(),
            FAKER.name().lastName(),
            FAKER.internet().emailAddress(),
            null, // customFields
            status == RegistrationStatus.CANCELLED ? Instant.now() : null, // cancelledAt
            status == RegistrationStatus.CANCELLED ? "Отменено пользователем" : null, // cancellationReason
            Instant.now() // createdAt
        );
    }
}
