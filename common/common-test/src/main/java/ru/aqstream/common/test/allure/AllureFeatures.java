package ru.aqstream.common.test.allure;

/**
 * Централизованные константы для Allure группировок.
 *
 * <p>Используется для аннотаций {@code @Epic}, {@code @Feature}, {@code @Story}
 * в тестах для группировки в Allure отчётах.</p>
 */
public final class AllureFeatures {

    // Epic уровень (типы тестов)
    public static final class Epics {
        public static final String UNIT_TESTS = "Unit Tests";
        public static final String INTEGRATION_TESTS = "Integration Tests";
        public static final String E2E_TESTS = "E2E Tests";

        private Epics() {
            // Утилитный класс
        }
    }

    // Feature уровень (фичи проекта)
    public static final class Features {
        public static final String USER_MANAGEMENT = "Управление пользователями";
        public static final String EVENT_MANAGEMENT = "Управление событиями";
        public static final String REGISTRATIONS = "Регистрации";
        public static final String CHECK_IN = "Check-in";
        public static final String NOTIFICATIONS = "Уведомления";
        public static final String ORGANIZATIONS = "Организации";
        public static final String TICKET_TYPES = "Типы билетов";
        public static final String PAYMENTS = "Платежи";
        public static final String ANALYTICS = "Аналитика";
        public static final String MEDIA = "Медиа";
        public static final String SECURITY = "Безопасность";

        private Features() {
            // Утилитный класс
        }
    }

    // Story уровень (подфичи для группировки)
    public static final class Stories {
        // User Management
        public static final String AUTHENTICATION = "Аутентификация";
        public static final String PROFILE = "Профиль";
        public static final String PASSWORD_RECOVERY = "Восстановление пароля";
        public static final String TELEGRAM_AUTH = "Telegram аутентификация";

        // Event Management
        public static final String EVENT_CRUD = "CRUD операции";
        public static final String EVENT_LIFECYCLE = "Жизненный цикл события";
        public static final String EVENT_PERMISSIONS = "Права доступа";

        // Registrations
        public static final String REGISTRATION_FLOW = "Регистрация участников";
        public static final String REGISTRATION_VALIDATION = "Валидация регистраций";

        // Organizations
        public static final String ORGANIZATION_REQUESTS = "Заявки на создание";
        public static final String ORGANIZATION_MEMBERS = "Управление членами";
        public static final String ORGANIZATION_INVITES = "Приглашения";

        // Check-in
        public static final String CHECK_IN_PROCESS = "Процесс check-in";
        public static final String CHECK_IN_VALIDATION = "Валидация check-in";

        // Ticket Types
        public static final String TICKET_TYPE_CRUD = "CRUD операции";
        public static final String TICKET_TYPE_VALIDATION = "Валидация типов билетов";

        // Notifications
        public static final String EMAIL_NOTIFICATIONS = "Email уведомления";
        public static final String TELEGRAM_NOTIFICATIONS = "Telegram уведомления";
        public static final String NOTIFICATION_PREFERENCES = "Настройки уведомлений";

        private Stories() {
            // Утилитный класс
        }
    }

    private AllureFeatures() {
        // Утилитный класс
    }
}
