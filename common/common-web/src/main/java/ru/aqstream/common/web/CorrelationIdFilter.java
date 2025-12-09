package ru.aqstream.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Фильтр для обработки X-Correlation-ID header.
 * Обеспечивает сквозной tracing запросов через все сервисы.
 *
 * <p>Если header отсутствует — генерируется новый UUID.
 * Correlation ID добавляется в MDC для логирования и в response header.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Добавляем в MDC для логирования
        MDC.put(MDC_CORRELATION_ID, correlationId);

        // Добавляем в response header
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Очищаем MDC после обработки запроса
            MDC.remove(MDC_CORRELATION_ID);
        }
    }

    /**
     * Возвращает текущий correlation ID из MDC.
     *
     * @return correlation ID или null если не установлен
     */
    public static String getCurrentCorrelationId() {
        return MDC.get(MDC_CORRELATION_ID);
    }
}
