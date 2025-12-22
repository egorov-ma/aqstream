package ru.aqstream.notification.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.user.client.UserClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TelegramMessageSender")
class TelegramMessageSenderTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private TelegramBot bot;

    @Mock
    private UserClient userClient;

    @InjectMocks
    private TelegramMessageSender sender;

    private Long chatId;
    private String text;

    @BeforeEach
    void setUp() {
        chatId = FAKER.number().numberBetween(100000000L, 999999999L);
        text = FAKER.lorem().paragraph();
    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessageTest {

        @Test
        @DisplayName("успешная отправка — возвращает true")
        void sendMessage_Success_ReturnsTrue() {
            // given
            SendResponse response = mockSuccessResponse();
            when(bot.execute(any(com.pengrad.telegrambot.request.SendMessage.class))).thenReturn(response);

            // when
            boolean result = sender.sendMessage(chatId, text);

            // then
            assertThat(result).isTrue();
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("ошибка 403 (blocked) — возвращает false без retry и очищает chat_id")
        void sendMessage_Blocked_ReturnsFalseAndClearsChatId() {
            // given
            SendResponse response = mockErrorResponse(403, "Forbidden: bot was blocked by the user");
            when(bot.execute(any(com.pengrad.telegrambot.request.SendMessage.class))).thenReturn(response);

            // when
            boolean result = sender.sendMessage(chatId, text);

            // then
            assertThat(result).isFalse();
            verify(bot, times(1)).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
            verify(userClient).clearTelegramChatId(String.valueOf(chatId));
        }

        @Test
        @DisplayName("ошибка 400 (chat not found) — возвращает false без retry и очищает chat_id")
        void sendMessage_ChatNotFound_ReturnsFalseAndClearsChatId() {
            // given
            SendResponse response = mockErrorResponse(400, "Bad Request: chat not found");
            when(bot.execute(any(com.pengrad.telegrambot.request.SendMessage.class))).thenReturn(response);

            // when
            boolean result = sender.sendMessage(chatId, text);

            // then
            assertThat(result).isFalse();
            verify(bot, times(1)).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
            verify(userClient).clearTelegramChatId(String.valueOf(chatId));
        }

        @Test
        @DisplayName("временная ошибка 500 — выполняет retry")
        void sendMessage_ServerError_Retries() {
            // given
            SendResponse errorResponse = mockErrorResponse(500, "Internal Server Error");
            SendResponse successResponse = mockSuccessResponse();
            when(bot.execute(any(com.pengrad.telegrambot.request.SendMessage.class)))
                    .thenReturn(errorResponse)
                    .thenReturn(successResponse);

            // when
            boolean result = sender.sendMessage(chatId, text);

            // then
            assertThat(result).isTrue();
            verify(bot, times(2)).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }
    }

    @Nested
    @DisplayName("sendPhoto")
    class SendPhotoTest {

        @Test
        @DisplayName("успешная отправка — возвращает true")
        void sendPhoto_Success_ReturnsTrue() {
            // given
            byte[] photo = FAKER.lorem().characters(100).getBytes();
            String caption = FAKER.lorem().sentence();
            SendResponse response = mockSuccessResponse();
            when(bot.execute(any(SendPhoto.class))).thenReturn(response);

            // when
            boolean result = sender.sendPhoto(chatId, photo, caption);

            // then
            assertThat(result).isTrue();
            verify(bot).execute(any(SendPhoto.class));
        }

        @Test
        @DisplayName("ошибка 403 (blocked) — возвращает false и очищает chat_id")
        void sendPhoto_Blocked_ReturnsFalseAndClearsChatId() {
            // given
            byte[] photo = FAKER.lorem().characters(100).getBytes();
            String caption = FAKER.lorem().sentence();
            SendResponse response = mockErrorResponse(403, "Forbidden: bot was blocked by the user");
            when(bot.execute(any(SendPhoto.class))).thenReturn(response);

            // when
            boolean result = sender.sendPhoto(chatId, photo, caption);

            // then
            assertThat(result).isFalse();
            verify(bot, times(1)).execute(any(SendPhoto.class));
            verify(userClient).clearTelegramChatId(String.valueOf(chatId));
        }

        @Test
        @DisplayName("ошибка 400 (chat not found) — возвращает false и очищает chat_id")
        void sendPhoto_ChatNotFound_ReturnsFalseAndClearsChatId() {
            // given
            byte[] photo = FAKER.lorem().characters(100).getBytes();
            String caption = FAKER.lorem().sentence();
            SendResponse response = mockErrorResponse(400, "Bad Request: chat not found");
            when(bot.execute(any(SendPhoto.class))).thenReturn(response);

            // when
            boolean result = sender.sendPhoto(chatId, photo, caption);

            // then
            assertThat(result).isFalse();
            verify(bot, times(1)).execute(any(SendPhoto.class));
            verify(userClient).clearTelegramChatId(String.valueOf(chatId));
        }
    }

    @Nested
    @DisplayName("sendMessageWithButtons")
    class SendMessageWithButtonsTest {

        @Test
        @DisplayName("с URL кнопкой — успешная отправка")
        void sendWithButtons_UrlButton_Success() {
            // given
            String[][] buttons = {{"Открыть", "https://example.com"}};
            SendResponse response = mockSuccessResponse();
            when(bot.execute(any(com.pengrad.telegrambot.request.SendMessage.class))).thenReturn(response);

            // when
            boolean result = sender.sendMessageWithButtons(chatId, text, buttons);

            // then
            assertThat(result).isTrue();
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }

        @Test
        @DisplayName("с callback кнопкой — успешная отправка")
        void sendWithButtons_CallbackButton_Success() {
            // given
            String[][] buttons = {{"Подтвердить", "confirm_action"}};
            SendResponse response = mockSuccessResponse();
            when(bot.execute(any(com.pengrad.telegrambot.request.SendMessage.class))).thenReturn(response);

            // when
            boolean result = sender.sendMessageWithButtons(chatId, text, buttons);

            // then
            assertThat(result).isTrue();
            verify(bot).execute(any(com.pengrad.telegrambot.request.SendMessage.class));
        }
    }

    private SendResponse mockSuccessResponse() {
        SendResponse response = mock(SendResponse.class);
        when(response.isOk()).thenReturn(true);
        return response;
    }

    private SendResponse mockErrorResponse(int code, String description) {
        SendResponse response = mock(SendResponse.class);
        when(response.isOk()).thenReturn(false);
        when(response.errorCode()).thenReturn(code);
        when(response.description()).thenReturn(description);
        return response;
    }
}
