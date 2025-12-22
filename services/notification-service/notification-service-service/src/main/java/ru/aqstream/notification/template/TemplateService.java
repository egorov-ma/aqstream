package ru.aqstream.notification.template;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.aqstream.notification.api.dto.NotificationChannel;
import ru.aqstream.notification.db.entity.NotificationTemplate;
import ru.aqstream.notification.db.repository.NotificationTemplateRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис для работы с шаблонами уведомлений.
 *
 * <p>Использует Mustache для подстановки переменных.
 * Кеширует скомпилированные шаблоны для производительности.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final NotificationTemplateRepository templateRepository;
    private final Mustache.Compiler mustacheCompiler = Mustache.compiler()
        .escapeHTML(false);  // Не экранируем HTML, используем Markdown

    // Кеш скомпилированных шаблонов: key = "code:channel"
    private final ConcurrentHashMap<String, Template> templateCache = new ConcurrentHashMap<>();

    /**
     * Рендерит шаблон с подстановкой переменных.
     *
     * @param code      код шаблона
     * @param channel   канал отправки
     * @param variables переменные для подстановки
     * @return отрендеренный текст
     * @throws TemplateNotFoundException если шаблон не найден
     */
    public String render(String code, NotificationChannel channel, Map<String, Object> variables) {
        NotificationTemplate templateEntity = findTemplate(code, channel);
        Template template = getCompiledTemplate(templateEntity);
        return template.execute(variables);
    }

    /**
     * Рендерит тему письма (для EMAIL).
     *
     * @param code      код шаблона
     * @param variables переменные для подстановки
     * @return отрендеренная тема или null
     */
    public String renderSubject(String code, Map<String, Object> variables) {
        Optional<NotificationTemplate> templateOpt =
            templateRepository.findByCodeAndChannel(code, NotificationChannel.EMAIL);

        if (templateOpt.isEmpty() || templateOpt.get().getSubject() == null) {
            return null;
        }

        String subject = templateOpt.get().getSubject();
        Template template = mustacheCompiler.compile(subject);
        return template.execute(variables);
    }

    /**
     * Проверяет существование шаблона.
     *
     * @param code    код шаблона
     * @param channel канал отправки
     * @return true если шаблон существует
     */
    public boolean templateExists(String code, NotificationChannel channel) {
        return templateRepository.existsByCodeAndChannel(code, channel);
    }

    /**
     * Получает шаблон по коду и каналу.
     *
     * @param code    код шаблона
     * @param channel канал отправки
     * @return шаблон или empty
     */
    public Optional<NotificationTemplate> getTemplate(String code, NotificationChannel channel) {
        return templateRepository.findByCodeAndChannel(code, channel);
    }

    /**
     * Очищает кеш шаблонов.
     * Вызывается при обновлении шаблонов в БД.
     */
    public void clearCache() {
        templateCache.clear();
        log.info("Кеш шаблонов очищен");
    }

    /**
     * Очищает кеш для конкретного шаблона.
     *
     * @param code    код шаблона
     * @param channel канал
     */
    public void evictFromCache(String code, NotificationChannel channel) {
        String cacheKey = buildCacheKey(code, channel);
        templateCache.remove(cacheKey);
        log.debug("Шаблон удалён из кеша: {}", cacheKey);
    }

    // === Приватные методы ===

    private NotificationTemplate findTemplate(String code, NotificationChannel channel) {
        return templateRepository.findByCodeAndChannel(code, channel)
            .orElseThrow(() -> new TemplateNotFoundException(code, channel));
    }

    private Template getCompiledTemplate(NotificationTemplate templateEntity) {
        String cacheKey = buildCacheKey(templateEntity.getCode(), templateEntity.getChannel());

        return templateCache.computeIfAbsent(cacheKey, key -> {
            log.debug("Компиляция шаблона: {}", key);
            return mustacheCompiler.compile(templateEntity.getBody());
        });
    }

    private String buildCacheKey(String code, NotificationChannel channel) {
        return code + ":" + channel.name();
    }
}
