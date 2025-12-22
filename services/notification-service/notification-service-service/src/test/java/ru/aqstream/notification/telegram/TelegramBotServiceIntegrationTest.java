package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.response.SendResponse;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.user.client.UserClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Интеграционные тесты для TelegramBotService.
 * Проверяет обработку команд бота в контексте Spring.
 */
@IntegrationTest
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("TelegramBotService Integration")
class TelegramBotServiceIntegrationTest {

    private static final Faker FAKER = new Faker();

    @MockitoBean
    private TelegramBot bot;

    @MockitoBean
    private UserClient userClient;

    @Autowired
    private TelegramBotService telegramBotService;

    private Long chatId;
    private Long telegramUserId;

    @BeforeEach
    void setUp() {
        chatId = FAKER.number().numberBetween(100000000L, 999999999L);
        telegramUserId = FAKER.number().numberBetween(100000000L, 999999999L);

        // Мокаем успешный ответ от Telegram API
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isOk()).thenReturn(true);
        when(bot.execute(any(com.pengrad.telegrambot.request.SendMessage.class))).thenReturn(successResponse);
    }

    @Nested
    @DisplayName("processUpdate")
    class ProcessUpdate {

        @Test
        @DisplayName("/start без параметров — отправляет приветствие")
        void processUpdate_StartCommand_SendsWelcome() {
            // given
            Update update = createUpdate("/start");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("/start с invite_ параметром — обрабатывает приглашение")
        void processUpdate_StartWithInvite_HandlesInvite() {
            // given
            String inviteCode = FAKER.regexify("[a-zA-Z0-9]{8}");
            Update update = createUpdate("/start invite_" + inviteCode);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("/start с link_ параметром — обрабатывает привязку")
        void processUpdate_StartWithLink_HandlesLink() {
            // given
            String linkToken = FAKER.regexify("[a-zA-Z0-9]{32}");
            Update update = createUpdate("/start link_" + linkToken);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("/start с reg_ параметром — обрабатывает регистрацию")
        void processUpdate_StartWithReg_HandlesRegistration() {
            // given
            String registrationId = FAKER.internet().uuid();
            Update update = createUpdate("/start reg_" + registrationId);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("/help — отправляет справку")
        void processUpdate_HelpCommand_SendsHelp() {
            // given
            Update update = createUpdate("/help");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("неизвестная команда — игнорируется")
        void processUpdate_UnknownCommand_Ignored() {
            // given
            Update update = createUpdate("/unknown");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot, times(0)).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("обычное сообщение — игнорируется")
        void processUpdate_RegularMessage_Ignored() {
            // given
            Update update = createUpdate("Привет, это обычное сообщение");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot, times(0)).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("сообщение без текста — игнорируется")
        void processUpdate_NoText_Ignored() {
            // given
            Update update = createUpdateWithoutText();

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(bot, times(0)).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }
    }

    /**
     * Создаёт Update с текстовым сообщением.
     */
    private Update createUpdate(String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mock(User.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn(text);
        when(message.chat()).thenReturn(chat);
        when(message.from()).thenReturn(user);
        when(chat.id()).thenReturn(chatId);
        when(user.id()).thenReturn(telegramUserId);
        when(user.firstName()).thenReturn(FAKER.name().firstName());

        return update;
    }

    /**
     * Создаёт Update без текста (например, фото).
     */
    private Update createUpdateWithoutText() {
        Update update = mock(Update.class);
        Message message = mock(Message.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn(null);

        return update;
    }
}
