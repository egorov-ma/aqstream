package ru.aqstream.gateway.filter;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ru.aqstream.gateway.GatewayHeaders;

/**
 * Фильтр для обработки X-Correlation-ID.
 * Генерирует ID если он отсутствует, добавляет в response и MDC для логирования.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    private static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders()
            .getFirst(GatewayHeaders.CORRELATION_ID);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            log.trace("Сгенерирован новый correlationId: {}", correlationId);
        }

        final String finalCorrelationId = correlationId;

        // Добавляем correlationId в request для downstream сервисов
        ServerHttpRequest request = exchange.getRequest().mutate()
            .header(GatewayHeaders.CORRELATION_ID, finalCorrelationId)
            .build();

        // Добавляем в response
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(GatewayHeaders.CORRELATION_ID, finalCorrelationId);

        return chain.filter(exchange.mutate().request(request).build())
            .contextWrite(ctx -> ctx.put(MDC_CORRELATION_ID, finalCorrelationId))
            .doOnSubscribe(subscription -> MDC.put(MDC_CORRELATION_ID, finalCorrelationId))
            .doFinally(signalType -> MDC.remove(MDC_CORRELATION_ID));
    }

    @Override
    public int getOrder() {
        // Выполняется самым первым
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
