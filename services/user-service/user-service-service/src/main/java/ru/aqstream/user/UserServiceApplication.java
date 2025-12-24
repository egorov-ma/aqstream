package ru.aqstream.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа для User Service.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {
    "ru.aqstream.user",
    "ru.aqstream.common.security",
    "ru.aqstream.common.web",
    "ru.aqstream.common.messaging"
})
@EntityScan(basePackages = {
    "ru.aqstream.user",
    "ru.aqstream.common.messaging"
})
@EnableJpaRepositories(basePackages = {
    "ru.aqstream.user",
    "ru.aqstream.common.messaging"
})
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
