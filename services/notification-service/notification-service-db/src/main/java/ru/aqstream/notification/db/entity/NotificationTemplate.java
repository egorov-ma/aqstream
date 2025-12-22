package ru.aqstream.notification.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.aqstream.common.data.BaseEntity;
import ru.aqstream.notification.api.dto.NotificationChannel;

import java.util.Map;

/**
 * Шаблон уведомления.
 *
 * <p>Шаблоны используют Mustache синтаксис для подстановки переменных
 * и Markdown для форматирования текста.</p>
 */
@Entity
@Table(name = "notification_templates", schema = "notification_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationTemplate extends BaseEntity {

    /**
     * Уникальный код шаблона (например, "registration.confirmed").
     */
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    /**
     * Канал отправки.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel = NotificationChannel.TELEGRAM;

    /**
     * Тема письма (только для EMAIL канала).
     */
    @Column(name = "subject", length = 255)
    private String subject;

    /**
     * Тело сообщения (Mustache + Markdown).
     */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * Описание переменных шаблона в формате {"varName": "описание"}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variables", nullable = false, columnDefinition = "jsonb")
    private Map<String, String> variables = Map.of();

    /**
     * Является ли шаблон системным (не редактируемый организаторами).
     */
    @Column(name = "is_system", nullable = false)
    private boolean isSystem = true;

    // === Фабричные методы ===

    /**
     * Создаёт системный шаблон для Telegram.
     *
     * @param code      уникальный код шаблона
     * @param body      тело сообщения
     * @param variables описание переменных
     * @return шаблон
     */
    public static NotificationTemplate createTelegramTemplate(String code, String body, Map<String, String> variables) {
        NotificationTemplate template = new NotificationTemplate();
        template.code = code;
        template.channel = NotificationChannel.TELEGRAM;
        template.body = body;
        template.variables = variables != null ? variables : Map.of();
        template.isSystem = true;
        return template;
    }

    /**
     * Создаёт системный шаблон для Email.
     *
     * @param code      уникальный код шаблона
     * @param subject   тема письма
     * @param body      тело сообщения
     * @param variables описание переменных
     * @return шаблон
     */
    public static NotificationTemplate createEmailTemplate(
            String code,
            String subject,
            String body,
            Map<String, String> variables) {
        NotificationTemplate template = new NotificationTemplate();
        template.code = code;
        template.channel = NotificationChannel.EMAIL;
        template.subject = subject;
        template.body = body;
        template.variables = variables != null ? variables : Map.of();
        template.isSystem = true;
        return template;
    }
}
