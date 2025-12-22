package ru.aqstream.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.aqstream.notification.config.NotificationProperties;
import ru.aqstream.notification.config.TelegramProperties;

/**
 * Точка входа для Notification Service.
 */
@SpringBootApplication
@EnableFeignClients(basePackages = "ru.aqstream.user.client")
@EnableScheduling
@EnableConfigurationProperties({TelegramProperties.class, NotificationProperties.class})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
