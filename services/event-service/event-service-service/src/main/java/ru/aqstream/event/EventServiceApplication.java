package ru.aqstream.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа для Event Service.
 * JPA конфигурация в JpaConfig.
 */
@SpringBootApplication
@EnableScheduling
@EnableFeignClients(basePackages = "ru.aqstream.user.client")
@ComponentScan(basePackages = {
    "ru.aqstream.event",
    "ru.aqstream.common.security",
    "ru.aqstream.common.web",
    "ru.aqstream.common.messaging"
})
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
