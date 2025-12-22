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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

            // when
            telegramBotService.processUpdate(update);

            // then
            verify(commandHandler, times(0)).handleStart(anyLong(), anyString(), any());
            verify(commandHandler, times(0)).handleHelp(anyLong());
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
}
