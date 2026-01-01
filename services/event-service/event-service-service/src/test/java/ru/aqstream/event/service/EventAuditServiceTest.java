package ru.aqstream.event.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.event.api.dto.EventAuditAction;
import ru.aqstream.event.api.dto.LocationType;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.EventAuditLog;
import ru.aqstream.event.db.repository.EventAuditLogRepository;

@DisplayName("EventAuditService")
@ExtendWith(MockitoExtension.class)
class EventAuditServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private EventAuditLogRepository auditLogRepository;

    @InjectMocks
    private EventAuditService eventAuditService;

    @Captor
    private ArgumentCaptor<EventAuditLog> auditLogCaptor;

    @Nested
    @DisplayName("logCreated")
    class LogCreated {

        @Test
        @DisplayName("сохраняет запись аудита с action CREATED")
        void logCreated_ValidEvent_SavesAuditLog() {
            // Given
            Event event = createTestEvent();

            // When
            eventAuditService.logCreated(event);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getEventId()).isEqualTo(event.getId());
            assertThat(savedLog.getAction()).isEqualTo(EventAuditAction.CREATED);
            assertThat(savedLog.getDescription()).isEqualTo("Событие создано");
        }
    }

    @Nested
    @DisplayName("logUpdated")
    class LogUpdated {

        @Test
        @DisplayName("сохраняет изменения при обновлении title")
        void logUpdated_TitleChanged_SavesChangedFields() {
            // Given
            UUID eventId = UUID.randomUUID();
            Instant startsAt = Instant.now().plusSeconds(86400);
            String slug = FAKER.internet().slug();
            String oldTitle = FAKER.lorem().sentence(3);

            // Создаём оба события с одинаковыми параметрами
            Event oldEvent = Event.create(oldTitle, slug, startsAt, "Europe/Moscow");
            Event newEvent = Event.create(oldTitle, slug, startsAt, "Europe/Moscow");

            // Меняем только title
            newEvent.updateInfo("Новое название", oldEvent.getDescription());

            // When
            eventAuditService.logUpdated(eventId, oldEvent, newEvent);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getAction()).isEqualTo(EventAuditAction.UPDATED);
            assertThat(savedLog.getChangedFields()).containsKey("title");
            assertThat(savedLog.getChangedFields()).hasSize(1);
            assertThat(savedLog.getDescription()).contains("Название");
        }

        @Test
        @DisplayName("не сохраняет запись если нет изменений")
        void logUpdated_NoChanges_DoesNotSave() {
            // Given
            UUID eventId = UUID.randomUUID();
            Event event = createTestEvent();

            // When
            eventAuditService.logUpdated(eventId, event, event);

            // Then
            verify(auditLogRepository, never()).save(any());
        }

        @Test
        @DisplayName("сохраняет множественные изменения")
        void logUpdated_MultipleChanges_SavesAllChangedFields() {
            // Given
            UUID eventId = UUID.randomUUID();
            Instant startsAt = Instant.now().plusSeconds(86400);
            String slug = FAKER.internet().slug();
            String oldTitle = FAKER.lorem().sentence(3);

            // Создаём оба события с одинаковыми параметрами
            Event oldEvent = Event.create(oldTitle, slug, startsAt, "Europe/Moscow");
            Event newEvent = Event.create(oldTitle, slug, startsAt, "Europe/Moscow");

            // Меняем несколько полей
            newEvent.updateInfo("Новое название", "Новое описание");
            newEvent.updateLocation(LocationType.ONLINE, null, "https://zoom.us/meeting");

            // When
            eventAuditService.logUpdated(eventId, oldEvent, newEvent);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getChangedFields()).hasSizeGreaterThan(1);
            assertThat(savedLog.getDescription()).contains("Изменено полей:");
        }
    }

    @Nested
    @DisplayName("logPublished")
    class LogPublished {

        @Test
        @DisplayName("сохраняет запись аудита с action PUBLISHED")
        void logPublished_ValidEvent_SavesAuditLog() {
            // Given
            Event event = createTestEvent();

            // When
            eventAuditService.logPublished(event);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getAction()).isEqualTo(EventAuditAction.PUBLISHED);
            assertThat(savedLog.getDescription()).isEqualTo("Событие опубликовано");
        }
    }

    @Nested
    @DisplayName("logUnpublished")
    class LogUnpublished {

        @Test
        @DisplayName("сохраняет запись аудита с action UNPUBLISHED")
        void logUnpublished_ValidEvent_SavesAuditLog() {
            // Given
            Event event = createTestEvent();

            // When
            eventAuditService.logUnpublished(event);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getAction()).isEqualTo(EventAuditAction.UNPUBLISHED);
            assertThat(savedLog.getDescription()).isEqualTo("Событие снято с публикации");
        }
    }

    @Nested
    @DisplayName("logCancelled")
    class LogCancelled {

        @Test
        @DisplayName("сохраняет запись с причиной отмены")
        void logCancelled_WithReason_SavesDescription() {
            // Given
            Event event = createTestEvent();
            String reason = "Форс-мажор";

            // When
            eventAuditService.logCancelled(event, reason);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getAction()).isEqualTo(EventAuditAction.CANCELLED);
            assertThat(savedLog.getDescription()).contains("Форс-мажор");
        }

        @Test
        @DisplayName("сохраняет запись без причины")
        void logCancelled_WithoutReason_SavesDefaultDescription() {
            // Given
            Event event = createTestEvent();

            // When
            eventAuditService.logCancelled(event, null);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getDescription()).isEqualTo("Событие отменено");
        }
    }

    @Nested
    @DisplayName("logCompleted")
    class LogCompleted {

        @Test
        @DisplayName("сохраняет запись аудита с action COMPLETED")
        void logCompleted_ValidEvent_SavesAuditLog() {
            // Given
            Event event = createTestEvent();

            // When
            eventAuditService.logCompleted(event);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getAction()).isEqualTo(EventAuditAction.COMPLETED);
            assertThat(savedLog.getDescription()).isEqualTo("Событие завершено");
        }
    }

    @Nested
    @DisplayName("logDeleted")
    class LogDeleted {

        @Test
        @DisplayName("сохраняет запись аудита с action DELETED")
        void logDeleted_ValidEvent_SavesAuditLog() {
            // Given
            Event event = createTestEvent();

            // When
            eventAuditService.logDeleted(event);

            // Then
            verify(auditLogRepository).save(auditLogCaptor.capture());
            EventAuditLog savedLog = auditLogCaptor.getValue();

            assertThat(savedLog.getAction()).isEqualTo(EventAuditAction.DELETED);
            assertThat(savedLog.getDescription()).isEqualTo("Событие удалено");
        }
    }

    /**
     * Создаёт тестовое событие.
     */
    private Event createTestEvent() {
        return Event.create(
            FAKER.lorem().sentence(3),
            FAKER.internet().slug(),
            Instant.now().plusSeconds(86400),
            "Europe/Moscow"
        );
    }
}
