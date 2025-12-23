package ru.aqstream.notification.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.aqstream.common.test.SecurityTestUtils.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.SharedServicesTestContainer;
import ru.aqstream.event.client.EventClient;
import ru.aqstream.notification.api.dto.UpdatePreferencesRequest;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.db.repository.NotificationPreferenceRepository;
import ru.aqstream.user.client.UserClient;

@IntegrationTest
@AutoConfigureMockMvc
@DisplayName("PreferencesController Integration Tests")
class PreferencesControllerIntegrationTest extends SharedServicesTestContainer {

    private static final String BASE_URL = "/api/v1/notifications/preferences";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    // Мокаем Feign клиенты, т.к. они не нужны для этого контроллера
    @MockitoBean
    @SuppressWarnings("unused")
    private UserClient userClient;

    @MockitoBean
    @SuppressWarnings("unused")
    private EventClient eventClient;

    private UUID userId;

    @BeforeEach
    void setUp() {
        preferenceRepository.deleteAll();
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("GET /api/v1/notifications/preferences")
    class GetPreferences {

        @Test
        @DisplayName("возвращает 401 без аутентификации")
        void getPreferences_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("возвращает настройки по умолчанию для нового пользователя")
        void getPreferences_NewUser_ReturnsDefaults() throws Exception {
            mockMvc.perform(get(BASE_URL)
                    .with(jwt(jwtTokenProvider, userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.event_reminder").value(true))
                .andExpect(jsonPath("$.settings.registration_updates").value(true))
                .andExpect(jsonPath("$.settings.event_changes").value(true))
                .andExpect(jsonPath("$.settings.organization_updates").value(true));

            // Проверяем что настройки сохранены в БД
            var saved = preferenceRepository.findByUserId(userId);
            assertThat(saved).isPresent();
            assertThat(saved.get().getSettings()).containsEntry("event_reminder", true);
        }

        @Test
        @DisplayName("возвращает существующие настройки пользователя")
        void getPreferences_ExistingUser_ReturnsExisting() throws Exception {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            pref.setSetting(NotificationPreference.EVENT_REMINDER, false);
            preferenceRepository.save(pref);

            // when & then
            mockMvc.perform(get(BASE_URL)
                    .with(jwt(jwtTokenProvider, userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.event_reminder").value(false))
                .andExpect(jsonPath("$.settings.registration_updates").value(true));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notifications/preferences")
    class UpdatePreferences {

        @Test
        @DisplayName("возвращает 401 без аутентификации")
        void updatePreferences_Unauthenticated_Returns401() throws Exception {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                Map.of("event_reminder", false)
            );

            mockMvc.perform(put(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("обновляет настройки для нового пользователя")
        void updatePreferences_NewUser_CreatesAndUpdates() throws Exception {
            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                Map.of(
                    "event_reminder", false,
                    "registration_updates", true
                )
            );

            mockMvc.perform(put(BASE_URL)
                    .with(jwt(jwtTokenProvider, userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.event_reminder").value(false))
                .andExpect(jsonPath("$.settings.registration_updates").value(true));

            // Проверяем сохранение в БД
            var saved = preferenceRepository.findByUserId(userId);
            assertThat(saved).isPresent();
            assertThat(saved.get().getSettings())
                .containsEntry("event_reminder", false)
                .containsEntry("registration_updates", true);
        }

        @Test
        @DisplayName("обновляет настройки для существующего пользователя")
        void updatePreferences_ExistingUser_Updates() throws Exception {
            // given
            NotificationPreference pref = NotificationPreference.createDefault(userId);
            preferenceRepository.save(pref);

            UpdatePreferencesRequest request = new UpdatePreferencesRequest(
                Map.of(
                    "event_reminder", false,
                    "event_changes", false
                )
            );

            // when & then
            mockMvc.perform(put(BASE_URL)
                    .with(jwt(jwtTokenProvider, userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settings.event_reminder").value(false))
                .andExpect(jsonPath("$.settings.event_changes").value(false))
                // Остальные настройки сохраняются
                .andExpect(jsonPath("$.settings.registration_updates").value(true))
                .andExpect(jsonPath("$.settings.organization_updates").value(true));
        }

        @Test
        @DisplayName("возвращает 400 при пустом запросе")
        void updatePreferences_NullSettings_Returns400() throws Exception {
            mockMvc.perform(put(BASE_URL)
                    .with(jwt(jwtTokenProvider, userId))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                .andExpect(status().isBadRequest());
        }
    }
}
