package ru.aqstream.notification.service;

import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.notification.api.dto.NotificationChannel;
import ru.aqstream.notification.api.dto.NotificationStatus;
import ru.aqstream.notification.db.entity.NotificationLog;
import ru.aqstream.notification.db.repository.NotificationLogRepository;
import ru.aqstream.notification.telegram.TelegramMessageSender;
import ru.aqstream.notification.template.TemplateService;
import ru.aqstream.user.api.dto.UserTelegramInfoDto;
import ru.aqstream.user.client.UserClient;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private TemplateService templateService;

    @Mock
    private TelegramMessageSender telegramSender;

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private PreferenceService preferenceService;

    @Mock
    private UserClient userClient;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationService notificationService;

    private UUID userId;
    private String templateCode;
    private String chatId;
    private String renderedBody;
    private Map<String, Object> variables;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        templateCode = "registration.confirmed";
        chatId = String.valueOf(FAKER.number().numberBetween(100000000L, 999999999L));
        renderedBody = FAKER.lorem().paragraph();
        variables = Map.of("firstName", FAKER.name().firstName());
    }

    private UserTelegramInfoDto createTelegramInfo(UUID userId, String chatId) {
        return new UserTelegramInfoDto(userId, chatId, FAKER.name().firstName(), FAKER.name().lastName());
    }

    @Nested
    @DisplayName("sendTelegram")
    class SendTelegram {

        @Test
        @DisplayName("успешно отправляет уведомление")
        void sendTelegram_Success_ReturnsTrue() {
            // given
            UserTelegramInfoDto telegramInfo = createTelegramInfo(userId, chatId);

            when(userClient.findTelegramInfo(userId)).thenReturn(Optional.of(telegramInfo));
            when(templateService.render(templateCode, NotificationChannel.TELEGRAM, variables))
                .thenReturn(renderedBody);
            when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(telegramSender.sendMessage(Long.parseLong(chatId), renderedBody)).thenReturn(true);

            // when
            boolean result = notificationService.sendTelegram(userId, templateCode, variables);

            // then
            assertThat(result).isTrue();
            // save вызывается дважды: создание лога + обновление статуса
            verify(logRepository, times(2)).save(any(NotificationLog.class));
        }

        @Test
        @DisplayName("возвращает false если пользователь не привязал Telegram")
        void sendTelegram_NoTelegram_ReturnsFalse() {
            // given
            when(userClient.findTelegramInfo(userId)).thenReturn(Optional.empty());

            // when
            boolean result = notificationService.sendTelegram(userId, templateCode, variables);

            // then
            assertThat(result).isFalse();
            verify(telegramSender, never()).sendMessage(anyLong(), anyString());
        }

        @Test
        @DisplayName("возвращает false если уведомление отключено пользователем")
        void sendTelegram_NotificationDisabled_ReturnsFalse() {
            // given
            String settingKey = "registration_updates";
            when(preferenceService.isNotificationEnabled(userId, settingKey)).thenReturn(false);

            // when
            boolean result = notificationService.sendTelegram(userId, templateCode, variables, settingKey);

            // then
            assertThat(result).isFalse();
            verify(userClient, never()).findTelegramInfo(any());
        }

        @Test
        @DisplayName("отправляет уведомление если настройка включена")
        void sendTelegram_NotificationEnabled_SendsMessage() {
            // given
            String settingKey = "registration_updates";
            UserTelegramInfoDto telegramInfo = createTelegramInfo(userId, chatId);

            when(preferenceService.isNotificationEnabled(userId, settingKey)).thenReturn(true);
            when(userClient.findTelegramInfo(userId)).thenReturn(Optional.of(telegramInfo));
            when(templateService.render(templateCode, NotificationChannel.TELEGRAM, variables))
                .thenReturn(renderedBody);
            when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(telegramSender.sendMessage(Long.parseLong(chatId), renderedBody)).thenReturn(true);

            // when
            boolean result = notificationService.sendTelegram(userId, templateCode, variables, settingKey);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("сохраняет лог с ошибкой при неудачной отправке")
        void sendTelegram_SendFails_SavesFailedLog() {
            // given
            UserTelegramInfoDto telegramInfo = createTelegramInfo(userId, chatId);

            when(userClient.findTelegramInfo(userId)).thenReturn(Optional.of(telegramInfo));
            when(templateService.render(templateCode, NotificationChannel.TELEGRAM, variables))
                .thenReturn(renderedBody);
            when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(telegramSender.sendMessage(Long.parseLong(chatId), renderedBody)).thenReturn(false);

            // when
            boolean result = notificationService.sendTelegram(userId, templateCode, variables);

            // then
            assertThat(result).isFalse();
            // save вызывается дважды: создание лога + обновление статуса
            verify(logRepository, times(2)).save(any(NotificationLog.class));
        }
    }

    @Nested
    @DisplayName("sendTelegramWithImage")
    class SendTelegramWithImage {

        @Test
        @DisplayName("успешно отправляет изображение")
        void sendTelegramWithImage_Success_ReturnsTrue() {
            // given
            byte[] image = FAKER.lorem().characters(100).getBytes();
            UserTelegramInfoDto telegramInfo = createTelegramInfo(userId, chatId);

            when(userClient.findTelegramInfo(userId)).thenReturn(Optional.of(telegramInfo));
            when(templateService.render(templateCode, NotificationChannel.TELEGRAM, variables))
                .thenReturn(renderedBody);
            when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(telegramSender.sendPhoto(Long.parseLong(chatId), image, renderedBody)).thenReturn(true);

            // when
            boolean result = notificationService.sendTelegramWithImage(userId, templateCode, variables, image);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("возвращает false если пользователь не привязал Telegram")
        void sendTelegramWithImage_NoTelegram_ReturnsFalse() {
            // given
            byte[] image = FAKER.lorem().characters(100).getBytes();
            when(userClient.findTelegramInfo(userId)).thenReturn(Optional.empty());

            // when
            boolean result = notificationService.sendTelegramWithImage(userId, templateCode, variables, image);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("sendEmail")
    class SendEmail {

        @Test
        @DisplayName("успешно отправляет email")
        void sendEmail_Success_ReturnsTrue() {
            // given
            String email = FAKER.internet().emailAddress();
            String subject = FAKER.lorem().sentence();

            when(templateService.renderSubject(templateCode, variables)).thenReturn(subject);
            when(templateService.render(templateCode, NotificationChannel.EMAIL, variables))
                .thenReturn(renderedBody);
            when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(emailService.send(email, subject, renderedBody)).thenReturn(true);

            // when
            boolean result = notificationService.sendEmail(userId, email, templateCode, variables);

            // then
            assertThat(result).isTrue();
            verify(emailService).send(email, subject, renderedBody);
        }

        @Test
        @DisplayName("сохраняет лог с ошибкой при неудачной отправке")
        void sendEmail_SendFails_SavesFailedLog() {
            // given
            String email = FAKER.internet().emailAddress();
            String subject = FAKER.lorem().sentence();

            when(templateService.renderSubject(templateCode, variables)).thenReturn(subject);
            when(templateService.render(templateCode, NotificationChannel.EMAIL, variables))
                .thenReturn(renderedBody);
            when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
            when(emailService.send(email, subject, renderedBody)).thenReturn(false);

            // when
            boolean result = notificationService.sendEmail(userId, email, templateCode, variables);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("shouldNotify")
    class ShouldNotify {

        @Test
        @DisplayName("делегирует проверку в PreferenceService")
        void shouldNotify_DelegatesToPreferenceService() {
            // given
            String settingKey = "event_reminder";
            when(preferenceService.isNotificationEnabled(userId, settingKey)).thenReturn(true);

            // when
            boolean result = notificationService.shouldNotify(userId, settingKey);

            // then
            assertThat(result).isTrue();
            verify(preferenceService).isNotificationEnabled(userId, settingKey);
        }
    }

    @Nested
    @DisplayName("getPendingCount / getFailedCount")
    class CountMethods {

        @Test
        @DisplayName("getPendingCount возвращает количество pending уведомлений")
        void getPendingCount_ReturnsCount() {
            // given
            when(logRepository.countByStatus(NotificationStatus.PENDING)).thenReturn(5L);

            // when
            long result = notificationService.getPendingCount();

            // then
            assertThat(result).isEqualTo(5L);
        }

        @Test
        @DisplayName("getFailedCount возвращает количество failed уведомлений")
        void getFailedCount_ReturnsCount() {
            // given
            when(logRepository.countByStatus(NotificationStatus.FAILED)).thenReturn(3L);

            // when
            long result = notificationService.getFailedCount();

            // then
            assertThat(result).isEqualTo(3L);
        }
    }
}
