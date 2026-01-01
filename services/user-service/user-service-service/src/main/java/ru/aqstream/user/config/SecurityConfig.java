package ru.aqstream.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import ru.aqstream.common.api.ErrorResponse;
import ru.aqstream.common.security.JwtAuthenticationFilter;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.web.TenantContextFilter;

/**
 * Конфигурация безопасности для User Service.
 *
 * <p>Эндпоинты аутентификации (/api/v1/auth/**) открыты,
 * остальные защищены JWT токеном.</p>
 *
 * <p>JWT валидируется через JwtAuthenticationFilter, что позволяет
 * сервису работать автономно и поддерживает полноценное интеграционное тестирование.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;
    private final TenantContextFilter tenantContextFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            // CSRF не нужен для stateless API
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless сессии (JWT)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Обработка ошибок аутентификации (возвращает 401)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint())
            )

            // Правила авторизации
            .authorizeHttpRequests(auth -> auth
                // Публичные эндпоинты аутентификации
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Internal endpoints для межсервисного взаимодействия
                // (защита на уровне сети, не доступны извне)
                .requestMatchers("/api/v1/internal/**").permitAll()
                // Системная информация (версия сервиса)
                .requestMatchers("/api/v1/system/**").permitAll()
                // Swagger UI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Actuator endpoints
                .requestMatchers("/actuator/**").permitAll()
                // Остальные требуют аутентификации
                .anyRequest().authenticated()
            )

            // JWT фильтр для валидации токенов
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            )

            // TenantContext фильтр — устанавливает tenant из JWT после аутентификации
            .addFilterAfter(tenantContextFilter, JwtAuthenticationFilter.class)

            .build();
    }

    /**
     * Обработчик ошибок аутентификации.
     * Возвращает 401 Unauthorized с JSON ответом.
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ErrorResponse errorResponse = new ErrorResponse(
                "unauthorized",
                "Требуется аутентификация"
            );

            try {
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            } catch (IOException e) {
                // Если не удалось записать JSON, отправляем текст
                response.getWriter().write("{\"code\":\"unauthorized\",\"message\":\"Требуется аутентификация\"}");
            }
        };
    }
}
