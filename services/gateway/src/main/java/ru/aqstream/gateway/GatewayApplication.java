package ru.aqstream.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.aqstream.gateway.version.ServiceEndpoints;

/**
 * Точка входа для API Gateway.
 */
@SpringBootApplication
@EnableConfigurationProperties(ServiceEndpoints.class)
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
