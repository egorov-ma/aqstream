package ru.aqstream.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа для User Service.
 * JPA конфигурация в JpaConfig.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "ru.aqstream.user",
    "ru.aqstream.common.security",
    "ru.aqstream.common.web",
    "ru.aqstream.common.messaging"
})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
