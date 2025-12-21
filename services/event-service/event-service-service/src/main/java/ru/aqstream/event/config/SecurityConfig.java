package ru.aqstream.event.config;

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

/**
 * Конфигурация безопасности для Event Service.
 *
 * <p>Все эндпоинты требуют аутентификации через JWT токен.</p>
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
                // Actuator endpoints
                .requestMatchers("/actuator/**").permitAll()
                // Публичные события (без авторизации)
                .requestMatchers("/api/v1/public/**").permitAll()
                // Swagger/OpenAPI
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                // Остальные требуют аутентификации
                .anyRequest().authenticated()
            )

            // JWT фильтр для валидации токенов
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            )

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
