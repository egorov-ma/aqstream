package ru.aqstream.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.aqstream.notification.config.NotificationProperties;
import ru.aqstream.notification.config.TelegramProperties;

/**
 * Точка входа для Notification Service.
 */
@SpringBootApplication
@EnableFeignClients(basePackages = {"ru.aqstream.user.client", "ru.aqstream.event.client"})
@EnableScheduling
@EnableConfigurationProperties({TelegramProperties.class, NotificationProperties.class})
@ComponentScan(basePackages = {
    "ru.aqstream.notification",
    "ru.aqstream.common.security",
    "ru.aqstream.common.web"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
