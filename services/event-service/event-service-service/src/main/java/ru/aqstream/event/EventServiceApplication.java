package ru.aqstream.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Точка входа для Event Service.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "ru.aqstream.event",
    "ru.aqstream.common.security",
    "ru.aqstream.common.web"
})
public class EventServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventServiceApplication.class, args);
    }
}
