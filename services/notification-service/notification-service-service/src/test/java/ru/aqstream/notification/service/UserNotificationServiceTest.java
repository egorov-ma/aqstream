package ru.aqstream.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.notification.api.dto.UserNotificationDto;
import ru.aqstream.notification.api.dto.UserNotificationType;
import ru.aqstream.notification.api.exception.UserNotificationNotFoundException;
import ru.aqstream.notification.db.entity.UserNotification;
import ru.aqstream.notification.db.repository.UserNotificationRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserNotificationService")
class UserNotificationServiceTest {

    @Mock
    private UserNotificationRepository repository;

    private UserNotificationService service;

    private static final Faker FAKER = new Faker();

    private UUID tenantId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new UserNotificationService(repository);
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("возвращает список уведомлений")
        void getNotifications_WithData_ReturnsList() {
            // given
            UserNotification notification = createTestNotification();
            Page<UserNotification> page = new PageImpl<>(List.of(notification));

            when(repository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
                eq(tenantId), eq(userId), any(Pageable.class)))
                .thenReturn(page);

            // when
            PageResponse<UserNotificationDto> result = service.getNotifications(userId, 0, 20);

            // then
            assertThat(result.data()).hasSize(1);
            assertThat(result.data().get(0).type()).isEqualTo(UserNotificationType.NEW_REGISTRATION);
        }

        @Test
        @DisplayName("возвращает пустой список для нового пользователя")
        void getNotifications_EmptyList_ReturnsEmpty() {
            // given
            when(repository.findByTenantIdAndUserIdOrderByCreatedAtDesc(
                eq(tenantId), eq(userId), any(Pageable.class)))
                .thenReturn(Page.empty());

            // when
            PageResponse<UserNotificationDto> result = service.getNotifications(userId, 0, 20);

            // then
            assertThat(result.data()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("возвращает количество непрочитанных")
        void getUnreadCount_ReturnsCount() {
            // given
            when(repository.countUnreadByTenantIdAndUserId(tenantId, userId)).thenReturn(5L);

            // when
            long count = service.getUnreadCount(userId);

            // then
            assertThat(count).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("отмечает уведомление как прочитанное")
        void markAsRead_ValidNotification_MarksAsRead() {
            // given
            UUID notificationId = UUID.randomUUID();
            UserNotification notification = createTestNotification();
            setNotificationId(notification, notificationId);

            when(repository.findByIdAndTenantIdAndUserId(notificationId, tenantId, userId))
                .thenReturn(Optional.of(notification));

            // when
            service.markAsRead(userId, notificationId);

            // then
            assertThat(notification.isRead()).isTrue();
            verify(repository).save(notification);
        }

        @Test
        @DisplayName("выбрасывает исключение для несуществующего уведомления")
        void markAsRead_NotFound_ThrowsException() {
            // given
            UUID notificationId = UUID.randomUUID();

            when(repository.findByIdAndTenantIdAndUserId(notificationId, tenantId, userId))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> service.markAsRead(userId, notificationId))
                .isInstanceOf(UserNotificationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("отмечает все уведомления как прочитанные")
        void markAllAsRead_ReturnsCount() {
            // given
            when(repository.markAllAsReadByTenantIdAndUserId(tenantId, userId)).thenReturn(10);

            // when
            int count = service.markAllAsRead(userId);

            // then
            assertThat(count).isEqualTo(10);
            verify(repository).markAllAsReadByTenantIdAndUserId(tenantId, userId);
        }
    }

    private UserNotification createTestNotification() {
        return UserNotification.createNewRegistration(
            tenantId,
            userId,
            UUID.randomUUID(),
            FAKER.name().fullName()
        );
    }

    private void setNotificationId(UserNotification notification, UUID id) {
        try {
            var idField = notification.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notification, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
