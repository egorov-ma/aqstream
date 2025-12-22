package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.model.User;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.notification.config.TelegramProperties;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramCommandHandler")
class TelegramCommandHandlerTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private TelegramMessageSender messageSender;

    @Mock
    private TelegramProperties properties;

    @Mock
    private DeeplinkHandler deeplinkHandler;

    @InjectMocks
    private TelegramCommandHandler commandHandler;

    private Long chatId;
    private User telegramUser;

    @BeforeEach
    void setUp() {
        chatId = FAKER.number().numberBetween(100000000L, 999999999L);
        telegramUser = mock(User.class);
        lenient().when(telegramUser.id()).thenReturn(FAKER.number().numberBetween(100000000L, 999999999L));
        lenient().when(telegramUser.firstName()).thenReturn(FAKER.name().firstName());
    }

    @Nested
    @DisplayName("handleStart")
    class HandleStart {

        @Test
        @DisplayName("без параметров — отправляет приветственное сообщение")
        void start_WithoutParams_SendsWelcome() {
            // when
            commandHandler.handleStart(chatId, "/start", telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Привет"));
        }

        @Test
        @DisplayName("с пробелами — отправляет приветственное сообщение")
        void start_WithSpaces_SendsWelcome() {
            // when
            commandHandler.handleStart(chatId, "/start   ", telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Привет"));
        }

        @Test
        @DisplayName("с invite_ — вызывает DeeplinkHandler.handleInvite")
        void start_WithInvite_CallsDeeplinkHandler() {
            // given
            String inviteCode = FAKER.regexify("[a-zA-Z0-9]{8}");

            // when
            commandHandler.handleStart(chatId, "/start invite_" + inviteCode, telegramUser);

            // then
            verify(deeplinkHandler).handleInvite(chatId, inviteCode, telegramUser);
        }

        @Test
        @DisplayName("с link_ — вызывает DeeplinkHandler.handleLink")
        void start_WithLink_CallsDeeplinkHandler() {
            // given
            String linkToken = FAKER.regexify("[a-zA-Z0-9]{32}");

            // when
            commandHandler.handleStart(chatId, "/start link_" + linkToken, telegramUser);

            // then
            verify(deeplinkHandler).handleLink(chatId, linkToken, telegramUser);
        }

        @Test
        @DisplayName("с reg_ — вызывает DeeplinkHandler.handleRegistration")
        void start_WithReg_CallsDeeplinkHandler() {
            // given
            String registrationId = FAKER.internet().uuid();

            // when
            commandHandler.handleStart(chatId, "/start reg_" + registrationId, telegramUser);

            // then
            verify(deeplinkHandler).handleRegistration(chatId, registrationId, telegramUser);
        }

        @Test
        @DisplayName("с неизвестным параметром — отправляет приветствие")
        void start_WithUnknownParam_SendsWelcome() {
            // when
            commandHandler.handleStart(chatId, "/start unknown_param", telegramUser);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Привет"));
        }

        @Test
        @DisplayName("с null user — отправляет приветствие с 'пользователь'")
        void start_WithNullUser_SendsDefaultWelcome() {
            // when
            commandHandler.handleStart(chatId, "/start", null);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("пользователь"));
        }
    }

    @Nested
    @DisplayName("handleHelp")
    class HandleHelp {

        @Test
        @DisplayName("отправляет справочное сообщение")
        void help_SendsHelpMessage() {
            // when
            commandHandler.handleHelp(chatId);

            // then
            verify(messageSender).sendMessage(eq(chatId), contains("Помощь"));
            verify(messageSender).sendMessage(eq(chatId), contains("/start"));
            verify(messageSender).sendMessage(eq(chatId), contains("/help"));
        }
    }
}
