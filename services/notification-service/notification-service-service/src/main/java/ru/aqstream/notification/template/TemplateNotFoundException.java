package ru.aqstream.notification.template;

import ru.aqstream.common.api.exception.EntityNotFoundException;
import ru.aqstream.notification.api.dto.NotificationChannel;

/**
 * Исключение: шаблон уведомления не найден.
 */
public class TemplateNotFoundException extends EntityNotFoundException {

    public TemplateNotFoundException(String code, NotificationChannel channel) {
        super(
            "template_not_found",
            String.format("Шаблон '%s' для канала %s не найден", code, channel.name())
        );
    }
}
