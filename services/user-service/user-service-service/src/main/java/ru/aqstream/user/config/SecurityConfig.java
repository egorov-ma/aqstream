package ru.aqstream.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Конфигурация безопасности для User Service.
 *
 * <p>Эндпоинты аутентификации (/api/v1/auth/**) открыты,
 * остальные защищены JWT токеном (обрабатывается на Gateway).</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // CSRF не нужен для stateless API
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless сессии (JWT)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Правила авторизации
            .authorizeHttpRequests(auth -> auth
                // Публичные эндпоинты аутентификации
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Actuator endpoints
                .requestMatchers("/actuator/**").permitAll()
                // Остальные требуют аутентификации (JWT проверяется на Gateway)
                .anyRequest().authenticated()
            )

            .build();
    }
}
