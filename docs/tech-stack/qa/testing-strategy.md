# Testing Strategy

Стратегия тестирования платформы AqStream.

## Пирамида тестирования

```
        /\
       /  \       E2E Tests (5%)
      /----\      Критические user flows
     /      \
    /--------\    Integration Tests (25%)
   /          \   API, БД, сервисы
  /------------\
 /              \ Unit Tests (70%)
/----------------\ Бизнес-логика
```

## Backend Testing

### Unit Tests

Изолированные тесты бизнес-логики.

```java
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    
    @Mock
    private EventMapper eventMapper;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @InjectMocks
    private EventService eventService;

    @Test
    void publish_DraftEvent_ChangesStatusToPublished() {
        // Given
        Event event = createDraftEvent();
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(eventRepository.save(any())).thenReturn(event);
        when(eventMapper.toDto(any())).thenReturn(createEventDto());
        
        // When
        EventDto result = eventService.publish(event.getId());
        
        // Then
        assertThat(result.status()).isEqualTo(EventStatus.PUBLISHED);
        verify(eventPublisher).publish(any(EventPublishedEvent.class));
    }

    @Test
    void publish_PublishedEvent_ThrowsException() {
        // Given
        Event event = createPublishedEvent();
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        
        // When/Then
        assertThatThrownBy(() -> eventService.publish(event.getId()))
            .isInstanceOf(InvalidEventStateException.class)
            .hasMessageContaining("Только черновик можно опубликовать");
    }
}
```

### Integration Tests

Тесты с реальной БД и RabbitMQ.

```java
@SpringBootTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE)
class EventServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
    }

    @Autowired
    private EventService eventService;
    
    @Autowired
    private EventRepository eventRepository;

    @Test
    @Transactional
    void create_ValidRequest_PersistsEvent() {
        // Given
        CreateEventRequest request = new CreateEventRequest(
            "Test Event",
            "Description",
            Instant.now().plus(7, ChronoUnit.DAYS),
            null,
            "Europe/Moscow"
        );
        
        TenantContext.setTenantId(UUID.randomUUID());
        
        // When
        EventDto result = eventService.create(request);
        
        // Then
        assertThat(result.id()).isNotNull();
        
        Event persisted = eventRepository.findById(result.id()).orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo("Test Event");
        assertThat(persisted.getStatus()).isEqualTo(EventStatus.DRAFT);
    }
}
```

### API Tests (RestAssured)

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
class EventControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private String accessToken;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        accessToken = tokenProvider.generateAccessToken(createTestUser());
    }

    @Test
    void getEvent_Authenticated_ReturnsEvent() {
        UUID eventId = createTestEvent();
        
        given()
            .header("Authorization", "Bearer " + accessToken)
        .when()
            .get("/api/v1/events/{id}", eventId)
        .then()
            .statusCode(200)
            .body("id", equalTo(eventId.toString()))
            .body("status", equalTo("DRAFT"));
    }

    @Test
    void getEvent_Unauthenticated_Returns401() {
        given()
        .when()
            .get("/api/v1/events/{id}", UUID.randomUUID())
        .then()
            .statusCode(401);
    }

    @Test
    void createEvent_InvalidData_Returns400() {
        given()
            .header("Authorization", "Bearer " + accessToken)
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "",
                    "startsAt": "2020-01-01T00:00:00Z"
                }
                """)
        .when()
            .post("/api/v1/events")
        .then()
            .statusCode(400)
            .body("code", equalTo("validation_error"))
            .body("details.title", notNullValue())
            .body("details.startsAt", notNullValue());
    }
}
```

## Frontend Testing

### Unit Tests (Vitest)

```typescript
// lib/utils/format-date.test.ts
import { describe, it, expect } from 'vitest';
import { formatEventDate } from './format-date';

describe('formatEventDate', () => {
  it('formats date correctly', () => {
    const date = '2024-06-15T09:00:00Z';
    const timezone = 'Europe/Moscow';
    
    const result = formatEventDate(date, timezone);
    
    expect(result).toBe('15 июня 2024, 12:00');
  });
});
```

### Component Tests

```typescript
// components/features/events/event-card.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { EventCard } from './event-card';

describe('EventCard', () => {
  const mockEvent = {
    id: '1',
    title: 'Test Event',
    status: 'PUBLISHED',
    startsAt: '2024-06-15T09:00:00Z',
  };

  it('renders event title', () => {
    render(<EventCard event={mockEvent} />);
    
    expect(screen.getByText('Test Event')).toBeInTheDocument();
  });

  it('calls onRegister when button clicked', () => {
    const onRegister = vi.fn();
    render(<EventCard event={mockEvent} onRegister={onRegister} />);
    
    fireEvent.click(screen.getByRole('button', { name: /зарегистрироваться/i }));
    
    expect(onRegister).toHaveBeenCalledWith('1');
  });
});
```

### E2E Tests (Playwright)

```typescript
// e2e/registration.spec.ts
import { test, expect } from '@playwright/test';

test.describe('Event Registration', () => {
  test('user can register for a free event', async ({ page }) => {
    // Navigate to event page
    await page.goto('/events/test-event');
    
    // Verify event details
    await expect(page.getByRole('heading', { name: 'Test Event' })).toBeVisible();
    
    // Click register button
    await page.getByRole('button', { name: 'Зарегистрироваться' }).click();
    
    // Fill registration form
    await page.getByLabel('Имя').fill('Иван');
    await page.getByLabel('Фамилия').fill('Иванов');
    await page.getByLabel('Email').fill('ivan@example.com');
    
    // Submit
    await page.getByRole('button', { name: 'Подтвердить' }).click();
    
    // Verify success
    await expect(page.getByText('Регистрация подтверждена')).toBeVisible();
    await expect(page.getByText(/[A-Z0-9]{8}/)).toBeVisible(); // Confirmation code
  });

  test('shows error when tickets sold out', async ({ page }) => {
    await page.goto('/events/sold-out-event');
    
    await expect(page.getByText('Билеты распроданы')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Зарегистрироваться' })).toBeDisabled();
  });
});
```

## Test Coverage

### Требования

| Уровень | Минимум |
|---------|---------|
| Unit Tests | 80% |
| Integration Tests | Все критические пути |
| E2E Tests | Основные user flows |

### Измерение

```bash
# Backend
./gradlew test jacocoTestReport

# Frontend
pnpm test:coverage
```

## Test Data

### Fixtures

```java
// test/fixtures/EventFixtures.java
public class EventFixtures {

    public static Event draftEvent() {
        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Draft Event");
        event.setStatus(EventStatus.DRAFT);
        event.setStartsAt(Instant.now().plus(7, ChronoUnit.DAYS));
        return event;
    }

    public static Event publishedEvent() {
        Event event = draftEvent();
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        return event;
    }
}
```

### Test Builders

```java
public class EventBuilder {
    private UUID id = UUID.randomUUID();
    private String title = "Test Event";
    private EventStatus status = EventStatus.DRAFT;
    private Instant startsAt = Instant.now().plus(7, ChronoUnit.DAYS);
    
    public EventBuilder withStatus(EventStatus status) {
        this.status = status;
        return this;
    }
    
    public Event build() {
        Event event = new Event();
        event.setId(id);
        event.setTitle(title);
        event.setStatus(status);
        event.setStartsAt(startsAt);
        return event;
    }
}

// Использование
Event event = new EventBuilder()
    .withStatus(EventStatus.PUBLISHED)
    .build();
```

## Команды

```bash
# Backend
./gradlew test                    # Все тесты (unit + integration + e2e)
./gradlew unit                    # Только unit тесты
./gradlew integration             # Только integration тесты
./gradlew e2e                     # Только E2E тесты
./gradlew test --tests "*Service*" # Тесты по имени

# Frontend
pnpm test                         # Unit tests
pnpm test:watch                   # Watch mode
pnpm test:coverage                # С coverage
pnpm test:e2e                     # E2E tests
pnpm test:e2e --ui                # E2E с UI
```

## Дальнейшее чтение

- [CI/CD](../../operations/ci-cd.md) — пайплайны
- [Backend Architecture](../backend/architecture.md)
