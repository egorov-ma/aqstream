package ru.aqstream.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.db.repository.NotificationPreferenceRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PreferenceService")
class PreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @InjectMocks
    private PreferenceService preferenceService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("isNotificationEnabled")
    class IsNotificationEnabled {

        @Test
        @DisplayName("возвращает true если настройка включена")
        void isNotificationEnabled_SettingEnabled_ReturnsTrue() {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            pref.setSetting(NotificationPreference.EVENT_REMINDER, true);

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            // when
            boolean result = preferenceService.isNotificationEnabled(userId, NotificationPreference.EVENT_REMINDER);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("возвращает false если настройка отключена")
        void isNotificationEnabled_SettingDisabled_ReturnsFalse() {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            pref.setSetting(NotificationPreference.EVENT_REMINDER, false);

            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            // when
            boolean result = preferenceService.isNotificationEnabled(userId, NotificationPreference.EVENT_REMINDER);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("возвращает true по умолчанию если настройки не найдены")
        void isNotificationEnabled_NoPreferences_ReturnsTrue() {
            // given
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // when
            boolean result = preferenceService.isNotificationEnabled(userId, NotificationPreference.EVENT_REMINDER);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("возвращает true для неизвестной настройки")
        void isNotificationEnabled_UnknownSetting_ReturnsTrue() {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            // when
            boolean result = preferenceService.isNotificationEnabled(userId, "unknown_setting");

            // then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("getPreferences")
    class GetPreferences {

        @Test
        @DisplayName("возвращает существующие настройки")
        void getPreferences_Exists_ReturnsPreferences() {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            // when
            NotificationPreference result = preferenceService.getPreferences(userId);

            // then
            assertThat(result).isEqualTo(pref);
        }

        @Test
        @DisplayName("возвращает настройки по умолчанию если не найдены")
        void getPreferences_NotExists_ReturnsDefault() {
            // given
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());

            // when
            NotificationPreference result = preferenceService.getPreferences(userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.isEnabled(NotificationPreference.EVENT_REMINDER)).isTrue();
            assertThat(result.isEnabled(NotificationPreference.REGISTRATION_UPDATES)).isTrue();
        }
    }

    @Nested
    @DisplayName("getOrCreatePreferences")
    class GetOrCreatePreferences {

        @Test
        @DisplayName("возвращает существующие настройки без сохранения")
        void getOrCreatePreferences_Exists_ReturnsWithoutSave() {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));

            // when
            NotificationPreference result = preferenceService.getOrCreatePreferences(userId);

            // then
            assertThat(result).isEqualTo(pref);
            verify(preferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("создаёт и сохраняет новые настройки если не найдены")
        void getOrCreatePreferences_NotExists_CreatesAndSaves() {
            // given
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            NotificationPreference result = preferenceService.getOrCreatePreferences(userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            verify(preferenceRepository).save(any(NotificationPreference.class));
        }
    }

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferences {

        @Test
        @DisplayName("обновляет существующие настройки")
        void updatePreferences_Exists_Updates() {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
            when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Boolean> newSettings = Map.of(
                NotificationPreference.EVENT_REMINDER, false,
                NotificationPreference.REGISTRATION_UPDATES, true
            );

            // when
            NotificationPreference result = preferenceService.updatePreferences(userId, newSettings);

            // then
            assertThat(result.isEnabled(NotificationPreference.EVENT_REMINDER)).isFalse();
            assertThat(result.isEnabled(NotificationPreference.REGISTRATION_UPDATES)).isTrue();
            verify(preferenceRepository).save(pref);
        }

        @Test
        @DisplayName("создаёт новые настройки если не существуют")
        void updatePreferences_NotExists_Creates() {
            // given
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            Map<String, Boolean> newSettings = Map.of(
                NotificationPreference.EVENT_REMINDER, false
            );

            // when
            NotificationPreference result = preferenceService.updatePreferences(userId, newSettings);

            // then
            assertThat(result.isEnabled(NotificationPreference.EVENT_REMINDER)).isFalse();
            // Остальные настройки по умолчанию true
            assertThat(result.isEnabled(NotificationPreference.REGISTRATION_UPDATES)).isTrue();
        }
    }

    @Nested
    @DisplayName("setSetting")
    class SetSetting {

        @Test
        @DisplayName("устанавливает конкретную настройку")
        void setSetting_ValidKey_UpdatesSetting() {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
            when(preferenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            preferenceService.setSetting(userId, NotificationPreference.EVENT_REMINDER, false);

            // then
            assertThat(pref.isEnabled(NotificationPreference.EVENT_REMINDER)).isFalse();
            verify(preferenceRepository).save(pref);
        }
    }
}
