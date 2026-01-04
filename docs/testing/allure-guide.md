# Allure отчёты в AqStream

## Обзор

Allure Report используется для визуализации результатов тестирования с группировкой по фичам, severity и типам тестов.

## Группировка тестов

### Epic уровень

Автоматически добавляется композитными аннотациями:
- **Unit Tests** — `@UnitTest`
- **Integration Tests** — `@IntegrationTest`
- **E2E Tests** — `@E2ETest`

### Feature уровень

Фича проекта через `@Feature(AllureFeatures.Features.*)`:
- `USER_MANAGEMENT` — Управление пользователями
- `EVENT_MANAGEMENT` — Управление событиями
- `REGISTRATIONS` — Регистрации
- `CHECK_IN` — Check-in
- `NOTIFICATIONS` — Уведомления
- `ORGANIZATIONS` — Организации
- `TICKET_TYPES` — Типы билетов
- `PAYMENTS` — Платежи
- `ANALYTICS` — Аналитика
- `MEDIA` — Медиа
- `SECURITY` — Безопасность

### Story уровень

Подфича через `@Story(AllureFeatures.Stories.*)`:
- `AUTHENTICATION` — Аутентификация
- `PROFILE` — Профиль
- `PASSWORD_RECOVERY` — Восстановление пароля
- `TELEGRAM_AUTH` — Telegram аутентификация
- `EVENT_CRUD` — CRUD операции
- `EVENT_LIFECYCLE` — Жизненный цикл события
- `EVENT_PERMISSIONS` — Права доступа
- `REGISTRATION_FLOW` — Регистрация участников
- `REGISTRATION_VALIDATION` — Валидация регистраций
- `ORGANIZATION_REQUESTS` — Заявки на создание
- `ORGANIZATION_MEMBERS` — Управление членами

### Severity

Критичность теста:
- `@Severity(BLOCKER)` — критичные функции (auth, RLS)
- `@Severity(CRITICAL)` — важные функции (регистрации, публикация)
- `@Severity(NORMAL)` — обычные функции
- `@Severity(MINOR)` — второстепенные функции

## Примеры

### Unit тест

```java
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import ru.aqstream.common.test.UnitTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.test.allure.AllureSteps;
import ru.aqstream.common.test.allure.TestLogger;
import static io.qameta.allure.SeverityLevel.BLOCKER;

@UnitTest
@Feature(AllureFeatures.Features.USER_MANAGEMENT)
@DisplayName("AuthService")
class AuthServiceTest {

    @Nested
    @Story(AllureFeatures.Stories.AUTHENTICATION)
    @DisplayName("register")
    class Register {

        @Test
        @Severity(BLOCKER)
        @DisplayName("успешно регистрирует нового пользователя")
        void register_ValidRequest_ReturnsAuthResponse() {
            // Arrange
            TestLogger.debug("Создание тестовых данных для регистрации: email={}", email);

            RegisterRequest request = AllureSteps.createTestUser(email, () ->
                new RegisterRequest(email, password, firstName, lastName)
            );

            // Act
            AuthResponse response = AllureSteps.callService("AuthService", "register", () ->
                authService.register(request, userAgent, ip)
            );

            // Assert
            AllureSteps.verifyResponse(201, () -> {
                assertThat(response.accessToken()).isNotNull();
            });

            TestLogger.info("Пользователь зарегистрирован: userId={}", userId);
            TestLogger.attachJson("Auth Response", response);
        }
    }
}
```

### Integration тест

```java
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.Story;
import ru.aqstream.common.test.IntegrationTest;
import ru.aqstream.common.test.allure.AllureFeatures;
import ru.aqstream.common.test.allure.AllureSteps;
import ru.aqstream.common.test.allure.TestLogger;
import static io.qameta.allure.SeverityLevel.BLOCKER;
import static io.qameta.allure.SeverityLevel.CRITICAL;

@IntegrationTest
@AutoConfigureMockMvc
@Feature(AllureFeatures.Features.EVENT_MANAGEMENT)
@DisplayName("EventController Integration Tests")
class EventControllerIntegrationTest extends SharedServicesTestContainer {

    @Nested
    @Story(AllureFeatures.Stories.EVENT_CRUD)
    @DisplayName("POST /api/v1/events")
    class Create {

        @Test
        @Severity(BLOCKER)
        @DisplayName("создаёт событие с валидными данными")
        void create_ValidRequest_ReturnsCreated() throws Exception {
            TestLogger.attachJson("Create Event Request", request);

            mockMvc.perform(post(BASE_URL)
                    .with(userAuth())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

            TestLogger.info("Событие создано через API");
        }
    }
}
```

## Allure Steps

### Утилитные steps (AllureSteps)

```java
// Создание тестовых данных
User user = AllureSteps.createTestUser(email, () ->
    User.createWithEmail(email, hash, firstName, lastName)
);

// Вызов сервиса
AuthResponse response = AllureSteps.callService("AuthService", "register", () ->
    authService.register(request, userAgent, ip)
);

// Выполнение HTTP запроса
AllureSteps.performRequest("POST", "/api/v1/events", () -> {
    return mockMvc.perform(post(BASE_URL)
        .with(userAuth())
        .content(json))
        .andExpect(status().isCreated());
});

// Проверка ответа
AllureSteps.verifyResponse(201, () -> {
    assertThat(response.accessToken()).isNotNull();
});

// Проверка данных в БД
AllureSteps.verifyDatabase("Event", eventId, () -> {
    Event event = repository.findById(eventId).orElseThrow();
    assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
});

// Настройка мока
AllureSteps.setupMock("UserRepository", () -> {
    when(userRepository.existsByEmail(email)).thenReturn(false);
    when(userRepository.save(any())).thenReturn(savedUser);
});
```

### Lambda steps в тестах

```java
Allure.step("Проверить токены в ответе", () -> {
    assertThat(response.accessToken()).isEqualTo(TEST_ACCESS_TOKEN);
    assertThat(response.refreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
});

Allure.step("Создать тип билета", () -> {
    TicketType ticketType = TicketType.create(event, "VIP");
    ticketTypeRepository.save(ticketType);
});
```

## Unified логирование (TestLogger)

Пишет одновременно в Allure attachments и SLF4J:

```java
// Логирование с уровнями
TestLogger.info("Событие создано: eventId={}", eventId);
TestLogger.debug("Токены обновлены: userId={}", userId);
TestLogger.warn("Подозрительная активность: userId={}", userId);
TestLogger.error("Ошибка оплаты: registrationId={}", regId, exception);

// Attachments
TestLogger.attachJson("Request", requestObject);
TestLogger.attachText("SQL Query", query);
TestLogger.attachSql("Select Query", "SELECT * FROM events WHERE id = ?");
TestLogger.attachHtml("Response", htmlContent);
TestLogger.attachXml("SOAP Request", xmlContent);
```

## Frontend E2E тесты (Playwright)

### Allure helpers

```typescript
import { test, expect } from '@playwright/test';
import {
  attachScreenshot,
  attachApiRequests,
  attachConsoleErrors,
  attachJson
} from './helpers/allure';

test.describe('Auth Pages', () => {
  // Мониторим API и консоль для всех тестов
  test.beforeEach(async ({ page }) => {
    await attachApiRequests(page);
    await attachConsoleErrors(page);
  });

  test('displays login form', async ({ page }) => {
    await test.step('Проверить видимость формы', async () => {
      await expect(page.getByTestId('login-form')).toBeVisible();
      await attachScreenshot(page, 'Login Form');
    });

    await test.step('Проверить поля ввода', async () => {
      await expect(page.getByTestId('email-input')).toBeVisible();
      await expect(page.getByTestId('password-input')).toBeVisible();
    });
  });
});
```

### Доступные helpers

```typescript
// Скриншоты
await attachScreenshot(page, 'Screenshot Name');

// JSON данные
await attachJson('Response Data', responseObject);

// HTML страницы
await attachHtml(page, 'Current Page');

// Текстовая информация
await attachText('Error Message', errorText);

// Storage
await attachStorage(page); // localStorage + sessionStorage

// Автоматический мониторинг (в beforeEach)
await attachApiRequests(page);    // Все API запросы
await attachConsoleErrors(page);  // Ошибки консоли
```

## Запуск и просмотр отчётов

### Backend

```bash
# Запустить тесты
./gradlew test

# Собрать отчёт из всех модулей
./gradlew allureAggregateReport

# Открыть веб-отчёт
./gradlew allureServe
```

### Frontend

```bash
cd frontend

# Запустить E2E тесты
pnpm test:e2e

# Открыть Allure отчёт
pnpm exec allure serve allure-results
```

## Структура отчёта

```
📁 Allure Report
  📁 Epics
    📁 Unit Tests
      📁 Управление пользователями
        ├── Аутентификация (5 тестов)
        └── Профиль (3 теста)
      📁 Управление событиями
        ├── События (8 тестов)
        └── Типы билетов (4 теста)

    📁 Integration Tests
      📁 Управление пользователями
        └── Аутентификация (6 тестов)
      📁 Управление событиями
        ├── События (10 тестов)
        └── Публикация (3 теста)

    📁 E2E Tests
      ├── Auth Pages (7 тестов)
      └── Event Registration (9 тестов)

  📊 Severity
    🔴 Blocker (15 тестов)
    🟠 Critical (20 тестов)
    🟡 Normal (30 тестов)
    🟢 Minor (2 теста)
```

## Категории ошибок (Frontend)

Allure автоматически группирует упавшие тесты по категориям:

- **Validation Errors** — ошибки валидации полей
- **API Errors** — проблемы с API запросами
- **Timeout Errors** — таймауты загрузки
- **Authentication Errors** — проблемы с авторизацией
- **Element Not Found** — элементы не найдены на странице

Категории настраиваются в `frontend/allure-results/categories.json`.

## Best Practices

### Что логировать

| Событие | Уровень | Пример |
|---------|---------|--------|
| Создание тестовых данных | `debug` | `TestLogger.debug("Создание пользователя: email={}", email)` |
| Успешные операции | `info` | `TestLogger.info("Пользователь создан: userId={}", id)` |
| Важные проверки | `info` | `TestLogger.info("Событие опубликовано: eventId={}", id)` |
| Предупреждения | `warn` | `TestLogger.warn("Подозрительная активность")` |
| Ошибки с исключениями | `error` | `TestLogger.error("Ошибка API", exception)` |

### Что прикреплять (attachments)

- **Request/Response объекты** — `TestLogger.attachJson("Request", request)`
- **SQL запросы** — `TestLogger.attachSql("Query", sql)`
- **HTML ответы** — `TestLogger.attachHtml("Response", html)`
- **Скриншоты (Playwright)** — `await attachScreenshot(page, "Error State")`
- **API запросы (Playwright)** — автоматически через `attachApiRequests(page)`

### Severity Guidelines

- **BLOCKER** — критичные функции, без которых система не работает
  - Аутентификация (login, register)
  - Создание ключевых сущностей (события, пользователи)
  - RLS изоляция данных
  - Проверка прав доступа (403, 401)

- **CRITICAL** — важные функции, влияющие на бизнес
  - Регистрация на события
  - Публикация событий
  - Платежи
  - Обновление/удаление сущностей

- **NORMAL** — обычные функции
  - Валидации полей
  - Обновления профиля
  - Фильтрация и поиск

- **MINOR** — второстепенные функции
  - Форматирование данных
  - Служебные операции

## Troubleshooting

### Отчёт не генерируется (Backend)

```bash
# Проверить что Allure results создаются
ls -la build/allure-results/

# Очистить старые результаты
./gradlew clean

# Запустить тесты заново
./gradlew test allureServe
```

### Отчёт пустой (Frontend)

```bash
cd frontend

# Проверить наличие результатов
ls -la allure-results/

# Убедиться что Playwright записывает в Allure
grep allure-playwright package.json

# Запустить тесты заново
pnpm test:e2e
pnpm exec allure serve allure-results
```

### Steps не отображаются

- Убедись что используешь `AllureSteps` или `Allure.step()`
- Проверь что импорты корректные: `import io.qameta.allure.Step;`
- Для Playwright: используй `test.step()` вместо обычных `await`

### Attachments не видны

- Проверь что `TestLogger.attachJson()` вызывается после логики теста
- Для JSON: убедись что объект сериализуемый (нет циклических ссылок)
- Проверь что Jackson настроен (должен быть в `common-test/build.gradle.kts`)
