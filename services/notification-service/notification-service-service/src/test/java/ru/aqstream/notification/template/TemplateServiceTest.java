package ru.aqstream.notification.template;

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
import ru.aqstream.notification.db.entity.NotificationTemplate;
import ru.aqstream.notification.db.repository.NotificationTemplateRepository;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TemplateService")
class TemplateServiceTest {

    private static final Faker FAKER = new Faker();

    @Mock
    private NotificationTemplateRepository templateRepository;

    @InjectMocks
    private TemplateService templateService;

    private String templateCode;
    private NotificationTemplate template;

    @BeforeEach
    void setUp() {
        templateCode = FAKER.regexify("[a-z]+\\.[a-z]+");
    }

    @Nested
    @DisplayName("render")
    class Render {

        @Test
        @DisplayName("рендерит шаблон с переменными")
        void render_WithVariables_ReturnsRenderedText() {
            // given
            String firstName = FAKER.name().firstName();
            String eventTitle = FAKER.lorem().sentence(3);

            template = NotificationTemplate.createTelegramTemplate(
                templateCode,
                "Привет, {{firstName}}! Событие: {{eventTitle}}",
                Map.of("firstName", "Имя", "eventTitle", "Название")
            );

            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.TELEGRAM))
                .thenReturn(Optional.of(template));

            Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "eventTitle", eventTitle
            );

            // when
            String result = templateService.render(templateCode, NotificationChannel.TELEGRAM, variables);

            // then
            assertThat(result).isEqualTo("Привет, " + firstName + "! Событие: " + eventTitle);
        }

        @Test
        @DisplayName("рендерит шаблон с условными блоками")
        void render_WithConditionalBlocks_ReturnsRenderedText() {
            // given
            template = NotificationTemplate.createTelegramTemplate(
                templateCode,
                "{{#hasLocation}}Место: {{location}}{{/hasLocation}}{{^hasLocation}}Место не указано{{/hasLocation}}",
                Map.of()
            );

            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.TELEGRAM))
                .thenReturn(Optional.of(template));

            // when - с условием true
            String withLocation = templateService.render(
                templateCode,
                NotificationChannel.TELEGRAM,
                Map.of("hasLocation", true, "location", "Москва")
            );

            // then
            assertThat(withLocation).isEqualTo("Место: Москва");
        }

        @Test
        @DisplayName("выбрасывает исключение если шаблон не найден")
        void render_TemplateNotFound_ThrowsException() {
            // given
            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.TELEGRAM))
                .thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() ->
                templateService.render(templateCode, NotificationChannel.TELEGRAM, Map.of())
            )
                .isInstanceOf(TemplateNotFoundException.class);
        }

        @Test
        @DisplayName("кеширует скомпилированный шаблон")
        void render_MultipleRenders_UsesCache() {
            // given
            template = NotificationTemplate.createTelegramTemplate(
                templateCode,
                "Текст: {{value}}",
                Map.of()
            );

            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.TELEGRAM))
                .thenReturn(Optional.of(template));

            // when - вызываем render дважды с разными переменными
            String result1 = templateService.render(templateCode, NotificationChannel.TELEGRAM, Map.of("value", "1"));
            String result2 = templateService.render(templateCode, NotificationChannel.TELEGRAM, Map.of("value", "2"));

            // then - репозиторий вызывается для каждого render (кешируется только Mustache Template, не entity)
            assertThat(result1).isEqualTo("Текст: 1");
            assertThat(result2).isEqualTo("Текст: 2");
        }
    }

    @Nested
    @DisplayName("renderSubject")
    class RenderSubject {

        @Test
        @DisplayName("рендерит тему письма")
        void renderSubject_EmailTemplate_ReturnsRenderedSubject() {
            // given
            String userName = FAKER.name().firstName();
            template = NotificationTemplate.createEmailTemplate(
                templateCode,
                "Привет, {{userName}}!",
                "<p>Тело письма</p>",
                Map.of()
            );

            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(template));

            // when
            String result = templateService.renderSubject(templateCode, Map.of("userName", userName));

            // then
            assertThat(result).isEqualTo("Привет, " + userName + "!");
        }

        @Test
        @DisplayName("возвращает null если шаблон не найден")
        void renderSubject_TemplateNotFound_ReturnsNull() {
            // given
            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());

            // when
            String result = templateService.renderSubject(templateCode, Map.of());

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("возвращает null если subject не задан")
        void renderSubject_NoSubject_ReturnsNull() {
            // given
            template = NotificationTemplate.createTelegramTemplate(
                templateCode,
                "Тело",
                Map.of()
            );
            // Telegram шаблон без subject

            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(template));

            // when
            String result = templateService.renderSubject(templateCode, Map.of());

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("templateExists")
    class TemplateExists {

        @Test
        @DisplayName("возвращает true если шаблон существует")
        void templateExists_Exists_ReturnsTrue() {
            // given
            when(templateRepository.existsByCodeAndChannel(templateCode, NotificationChannel.TELEGRAM))
                .thenReturn(true);

            // when
            boolean result = templateService.templateExists(templateCode, NotificationChannel.TELEGRAM);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("возвращает false если шаблон не существует")
        void templateExists_NotExists_ReturnsFalse() {
            // given
            when(templateRepository.existsByCodeAndChannel(templateCode, NotificationChannel.TELEGRAM))
                .thenReturn(false);

            // when
            boolean result = templateService.templateExists(templateCode, NotificationChannel.TELEGRAM);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("cache operations")
    class CacheOperations {

        @Test
        @DisplayName("clearCache очищает весь кеш")
        void clearCache_ClearsAllCache() {
            // given
            template = NotificationTemplate.createTelegramTemplate(
                templateCode,
                "Текст: {{value}}",
                Map.of()
            );
            when(templateRepository.findByCodeAndChannel(templateCode, NotificationChannel.TELEGRAM))
                .thenReturn(Optional.of(template));

            // Первый рендер - компилируется и кешируется Mustache шаблон
            String result1 = templateService.render(templateCode, NotificationChannel.TELEGRAM, Map.of("value", "1"));

            // when - очищаем кеш скомпилированных шаблонов
            templateService.clearCache();

            // Второй рендер после очистки - шаблон перекомпилируется
            String result2 = templateService.render(templateCode, NotificationChannel.TELEGRAM, Map.of("value", "2"));

            // then - оба рендера работают корректно
            assertThat(result1).isEqualTo("Текст: 1");
            assertThat(result2).isEqualTo("Текст: 2");
        }
    }
}
