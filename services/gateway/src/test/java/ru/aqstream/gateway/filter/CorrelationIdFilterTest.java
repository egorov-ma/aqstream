package ru.aqstream.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import ru.aqstream.gateway.GatewayHeaders;

/**
 * Тесты для CorrelationIdFilter.
 */
class CorrelationIdFilterTest {

    private static final String CORRELATION_ID_HEADER = GatewayHeaders.CORRELATION_ID;

    private CorrelationIdFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new CorrelationIdFilter();
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("filter_WithExistingCorrelationId_PreservesIt")
    void filter_WithExistingCorrelationId_PreservesIt() {
        // Arrange
        String existingCorrelationId = UUID.randomUUID().toString();
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
            .header(CORRELATION_ID_HEADER, existingCorrelationId)
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        String responseCorrelationId = exchange.getResponse().getHeaders()
            .getFirst(CORRELATION_ID_HEADER);
        assertEquals(existingCorrelationId, responseCorrelationId);
    }

    @Test
    @DisplayName("filter_WithoutCorrelationId_GeneratesNew")
    void filter_WithoutCorrelationId_GeneratesNew() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        String responseCorrelationId = exchange.getResponse().getHeaders()
            .getFirst(CORRELATION_ID_HEADER);
        assertNotNull(responseCorrelationId);
        // Проверяем, что это валидный UUID
        UUID.fromString(responseCorrelationId);
    }

    @Test
    @DisplayName("filter_WithBlankCorrelationId_GeneratesNew")
    void filter_WithBlankCorrelationId_GeneratesNew() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
            .header(CORRELATION_ID_HEADER, "   ")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        String responseCorrelationId = exchange.getResponse().getHeaders()
            .getFirst(CORRELATION_ID_HEADER);
        assertNotNull(responseCorrelationId);
        assertTrue(!responseCorrelationId.isBlank());
    }

    @Test
    @DisplayName("getOrder_ReturnsHighestPrecedence")
    void getOrder_ReturnsHighestPrecedence() {
        // Act & Assert
        assertEquals(Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }

    @Test
    @DisplayName("filter_AddsCorrelationIdToResponse")
    void filter_AddsCorrelationIdToResponse() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        HttpHeaders responseHeaders = exchange.getResponse().getHeaders();
        assertTrue(responseHeaders.containsKey(CORRELATION_ID_HEADER));
    }
}
