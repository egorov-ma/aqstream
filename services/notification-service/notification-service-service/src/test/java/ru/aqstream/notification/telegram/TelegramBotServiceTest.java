package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import ru.aqstream.user.api.dto.ConfirmTelegramAuthRequest;
import ru.aqstream.user.client.UserClient;
import java.util.Collections;

/**
 * Unit тесты для TelegramBotService.
 * Проверяет обработку команд бота.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramBotService")
class TelegramBotServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private TelegramBot bot;

    @Mock
    private TelegramProperties properties;

    @Mock
    private TelegramCommandHandler commandHandler;

    @Mock
    private TelegramMessageSender messageSender;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private TelegramBotService telegramBotService;

    private Long chatId;
    private Long telegramUserId;

    @BeforeEach
    void setUp() {
        chatId = FAKER.number().numberBetween(100000000L, 999999999L);
        telegramUserId = FAKER.number().numberBetween(100000000L, 999999999L);
    }

    @Nested
    @DisplayName("processUpdate")
    class ProcessUpdate {

        @Test
        @DisplayName("/start без параметров — вызывает handleStart")
        void processUpdate_StartCommand_CallsHandleStart() {
            // given
            Update update = createUpdate("/start");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler).handleStart(anyLong(), anyString(), any(User.class));
        }

        @Test
        @DisplayName("/start с параметром — вызывает handleStart")
        void processUpdate_StartWithParam_CallsHandleStart() {
            // given
            String inviteCode = FAKER.regexify("[a-zA-Z0-9]{8}");
            Update update = createUpdate("/start invite_" + inviteCode);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler).handleStart(anyLong(), anyString(), any(User.class));
        }

        @Test
        @DisplayName("/help — вызывает handleHelp")
        void processUpdate_HelpCommand_CallsHandleHelp() {
            // given
            Update update = createUpdate("/help");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler).handleHelp(anyLong());
        }

        @Test
        @DisplayName("неизвестная команда — не вызывает обработчики")
        void processUpdate_UnknownCommand_NoHandlerCalled() {
            // given
            Update update = createUpdate("/unknown");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler, times(0)).handleStart(anyLong(), anyString(), any());
            verify(commandHandler, times(0)).handleHelp(anyLong());
        }

        @Test
        @DisplayName("обычное сообщение — игнорируется")
        void processUpdate_RegularMessage_Ignored() {
            // given
            Update update = createUpdate("Привет, это обычное сообщение");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler, times(0)).handleStart(anyLong(), anyString(), any());
            verify(commandHandler, times(0)).handleHelp(anyLong());
        }

        @Test
        @DisplayName("сообщение без текста — игнорируется")
        void processUpdate_NoText_Ignored() {
            // given
            Update update = createUpdateWithoutText();

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler, times(0)).handleStart(anyLong(), anyString(), any());
            verify(commandHandler, times(0)).handleHelp(anyLong());
        }

        @Test
        @DisplayName("update без message — игнорируется")
        void processUpdate_NoMessage_Ignored() {
            // given
            Update update = mock(Update.class);
            when(update.message()).thenReturn(null);
            when(update.callbackQuery()).thenReturn(null);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler, times(0)).handleStart(anyLong(), anyString(), any());
            verify(commandHandler, times(0)).handleHelp(anyLong());
        }

        @Test
        @DisplayName("callbackQuery с confirm_auth — вызывает userClient")
        void processUpdate_CallbackQuery_ConfirmAuth_CallsUserClient() {
            // given
            String token = "test-token-12345678";
            Update update = createCallbackUpdate("confirm_auth:" + token);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(userClient).confirmTelegramAuth(any(ConfirmTelegramAuthRequest.class));
            verify(bot).execute(any(AnswerCallbackQuery.class));
            verify(messageSender).sendMessage(eq(chatId), anyString());
        }

        @Test
        @DisplayName("callbackQuery с confirm_auth — успешно отправляет сообщение")
        void processUpdate_CallbackQuery_ConfirmAuth_SendsSuccessMessage() {
            // given
            String token = "test-token-12345678";
            Update update = createCallbackUpdate("confirm_auth:" + token);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(messageSender).sendMessage(eq(chatId),
                org.mockito.ArgumentMatchers.contains("Вход выполнен"));
        }

        @Test
        @DisplayName("callbackQuery с confirm_auth — ошибка 404 отправляет сообщение об устаревшей ссылке")
        void processUpdate_CallbackQuery_ConfirmAuth_NotFound_SendsExpiredMessage() {
            // given
            String token = "expired-token-12345";
            Update update = createCallbackUpdate("confirm_auth:" + token);

            Request request = Request.create(Request.HttpMethod.POST, "/test",
                Collections.emptyMap(), null, new RequestTemplate());
            FeignException.NotFound notFoundException =
                new FeignException.NotFound("Not found", request, null, null);

            doThrow(notFoundException).when(userClient).confirmTelegramAuth(any());

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(messageSender).sendMessage(eq(chatId),
                org.mockito.ArgumentMatchers.contains("устарела"));
        }

        @Test
        @DisplayName("callbackQuery с неизвестными данными — игнорируется")
        void processUpdate_CallbackQuery_UnknownData_Ignored() {
            // given
            Update update = createCallbackUpdate("unknown_action:data");

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(userClient, never()).confirmTelegramAuth(any());
            verify(messageSender, never()).sendMessage(anyLong(), anyString());
        }

        @Test
        @DisplayName("callbackQuery без data — игнорируется")
        void processUpdate_CallbackQuery_NullData_Ignored() {
            // given
            Update update = mock(Update.class);
            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            when(update.callbackQuery()).thenReturn(callbackQuery);
            when(callbackQuery.data()).thenReturn(null);

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(userClient, never()).confirmTelegramAuth(any());
        }
    }

    /**
     * Создаёт Update с текстовым сообщением.
     * Использует lenient stubbing для полей, которые не всегда используются.
     */
    private Update createUpdate(String text) {
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mock(User.class);

        when(update.message()).thenReturn(message);
        when(message.text()).thenReturn(text);
        when(message.chat()).thenReturn(chat);
        when(chat.id()).thenReturn(chatId);
        // Lenient stubbing для полей, которые используются только в некоторых тестах
        lenient().when(message.from()).thenReturn(user);
        lenient().when(user.id()).thenReturn(telegramUserId);
        lenient().when(user.firstName()).thenReturn(FAKER.name().firstName());

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

    /**
     * Создаёт Update с CallbackQuery (нажатие inline-кнопки).
     * Использует lenient stubbing для полей, которые могут не использоваться в некоторых тестах.
     */
    @SuppressWarnings("deprecation")
    private Update createCallbackUpdate(String callbackData) {
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        User user = mock(User.class);

        when(update.callbackQuery()).thenReturn(callbackQuery);
        lenient().when(update.message()).thenReturn(null);

        when(callbackQuery.data()).thenReturn(callbackData);
        lenient().when(callbackQuery.id()).thenReturn("callback-id-123");
        lenient().when(callbackQuery.from()).thenReturn(user);
        lenient().when(callbackQuery.message()).thenReturn(message);

        lenient().when(message.chat()).thenReturn(chat);
        lenient().when(chat.id()).thenReturn(chatId);

        lenient().when(user.id()).thenReturn(telegramUserId);
        lenient().when(user.firstName()).thenReturn(FAKER.name().firstName());
        lenient().when(user.lastName()).thenReturn(FAKER.name().lastName());
        lenient().when(user.username()).thenReturn(FAKER.internet().username());

        return update;
    }
}
