package ru.aqstream.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Фильтр для логирования HTTP запросов и ответов.
 * Логирует: метод, URI, статус, время выполнения.
 */
@Component
@Order(10) // После CorrelationIdFilter
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    private static final String MDC_REQUEST_METHOD = "httpMethod";
    private static final String MDC_REQUEST_URI = "requestUri";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        // Пропускаем actuator endpoints для уменьшения логов
        String uri = request.getRequestURI();
        if (uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String method = request.getMethod();

        // Добавляем в MDC для structured logging
        MDC.put(MDC_REQUEST_METHOD, method);
        MDC.put(MDC_REQUEST_URI, uri);

        long startTime = System.currentTimeMillis();

        try {
            log.debug("Входящий запрос: {} {}", method, uri);
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            if (status >= 500) {
                log.error("Ответ: {} {} -> {} ({}ms)", method, uri, status, duration);
            } else if (status >= 400) {
                log.warn("Ответ: {} {} -> {} ({}ms)", method, uri, status, duration);
            } else {
                log.info("Ответ: {} {} -> {} ({}ms)", method, uri, status, duration);
            }

            MDC.remove(MDC_REQUEST_METHOD);
            MDC.remove(MDC_REQUEST_URI);
        }
    }
}
