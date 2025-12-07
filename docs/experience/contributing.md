# Contributing Guide

Руководство по участию в разработке AqStream.

## Начало работы

### Требования

- Docker 24+ и Docker Compose v2
- JDK 21
- Node.js 20 LTS
- pnpm 8+
- Git 2.40+

### Клонирование и запуск

```bash
# Клонировать репозиторий
git clone https://github.com/aqstream/aqstream.git
cd aqstream

# Скопировать конфигурацию
cp .env.example .env

# Запустить всё
make up

# Проверить
curl http://localhost:8080/actuator/health
```

Подробнее: [Environments](../operations/environments.md)

## Workflow

### 1. Выбрать задачу

- Посмотреть [Issues](https://github.com/aqstream/aqstream/issues)
- Выбрать задачу с меткой `good first issue` или `help wanted`
- Написать комментарий что берёте в работу

### 2. Создать ветку

```bash
# Формат: type/short-description
git checkout -b feature/add-waitlist
git checkout -b fix/registration-validation
git checkout -b docs/api-examples
```

### 3. Внести изменения

Следуя [Code Style](#code-style) и архитектуре проекта.

### 4. Написать тесты

- Unit tests для бизнес-логики
- Integration tests для API
- Покрытие должно быть ≥80%

### 5. Проверить локально

```bash
# Backend
./gradlew test
./gradlew checkstyleMain

# Frontend
cd frontend
pnpm lint
pnpm test
pnpm build
```

### 6. Создать Pull Request

```markdown
## Описание
[Что сделано и зачем]

## Тип изменения
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation

## Чеклист
- [ ] Код соответствует стилю проекта
- [ ] Тесты написаны и проходят
- [ ] Документация обновлена (если нужно)
- [ ] Self-review проведён
```

### 7. Code Review

- Дождаться review от maintainer
- Внести правки по комментариям
- После approve — merge

## Code Style

### Backend (Java)

- Google Java Style с модификациями
- 4 spaces indentation
- Max line length: 120
- Constructor injection

```java
// ✅ Правильно
@Service
@RequiredArgsConstructor
public class EventService {
    
    private final EventRepository eventRepository;
    
    public EventDto findById(UUID id) {
        return eventRepository.findById(id)
            .map(eventMapper::toDto)
            .orElseThrow(() -> new EventNotFoundException(id));
    }
}

// ❌ Неправильно
@Service
public class EventService {
    
    @Autowired  // Field injection
    private EventRepository eventRepository;
}
```

### Frontend (TypeScript)

- ESLint + Prettier
- 2 spaces indentation
- Named exports (кроме pages)
- Strict TypeScript

```typescript
// ✅ Правильно
export function EventCard({ event }: EventCardProps) {
  return (
    <Card>
      <CardTitle>{event.title}</CardTitle>
    </Card>
  );
}

// ❌ Неправильно
export default function EventCard({ event }: any) { ... }
```

### Язык

| Контекст | Язык |
|----------|------|
| Код (переменные, классы, методы) | English |
| Комментарии | Русский |
| Сообщения об ошибках | Русский |
| Git commits | English |
| Документация | Русский |

## Commit Messages

```
type(scope): short description

[optional body]

[optional footer]
```

**Types:**
- `feat` — новая функциональность
- `fix` — исправление бага
- `docs` — документация
- `refactor` — рефакторинг
- `test` — тесты
- `chore` — прочее

**Примеры:**

```
feat(event): add waitlist support

fix(registration): validate email uniqueness

docs(api): add pagination examples

refactor(user-service): extract auth logic to separate class
```

## Архитектура

Перед внесением изменений ознакомьтесь с:

- [Architecture Overview](../architecture/overview.md)
- [Backend Architecture](../tech-stack/backend/architecture.md)
- [Frontend Architecture](../tech-stack/frontend/architecture.md)

### Ключевые принципы

1. **Spring MVC** — не WebFlux (кроме Gateway)
2. **shadcn/ui** — единственная UI библиотека
3. **Schema-per-service** — каждый сервис владеет своими данными
4. **Outbox pattern** — для событий между сервисами

## Тестирование

### Backend

```java
// Unit test
@ExtendWith(MockitoExtension.class)
class EventServiceTest {
    @Mock private EventRepository eventRepository;
    @InjectMocks private EventService eventService;
    
    @Test
    void publish_DraftEvent_Success() { ... }
}

// Integration test
@SpringBootTest
@Testcontainers
class EventControllerTest { ... }
```

### Frontend

```typescript
// Component test
describe('EventCard', () => {
  it('renders event title', () => {
    render(<EventCard event={mockEvent} />);
    expect(screen.getByText('Test Event')).toBeInTheDocument();
  });
});
```

## Pull Request

### Размер

- Маленькие PR (< 400 строк) предпочтительнее
- Большие изменения разбивать на части

### Review

- Будьте конструктивны
- Объясняйте "почему", не только "что"
- Отвечайте на все комментарии

### После merge

- Удалите ветку
- Закройте связанный issue
- Проверьте CI/CD

## Документация

Если ваши изменения затрагивают:

- API — обновите OpenAPI annotations
- Конфигурацию — обновите `.env.example`
- Архитектуру — обновите документацию в `docs/`

## Вопросы

- GitHub Discussions для вопросов
- Issues для багов и feature requests
- Pull Requests для code review

## Code of Conduct

- Уважайте других участников
- Конструктивная критика
- Инклюзивность

## Благодарность

Спасибо за участие в развитии AqStream! 🎉
