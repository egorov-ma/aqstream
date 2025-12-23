package ru.aqstream.gateway.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import ru.aqstream.gateway.security.JwtTokenValidator;
import ru.aqstream.gateway.security.JwtValidationException;

/**
 * Тесты для JwtAuthenticationFilter.
 */
class JwtAuthenticationFilterTest {

    private static final Faker FAKER = new Faker();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String EMAIL = FAKER.internet().emailAddress();
    private static final Set<String> ROLES = Set.of("ROLE_USER");
    private static final String VALID_TOKEN = "valid.jwt.token";

    private JwtAuthenticationFilter filter;
    private JwtTokenValidator tokenValidator;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        tokenValidator = mock(JwtTokenValidator.class);
        filter = new JwtAuthenticationFilter(tokenValidator);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    @DisplayName("filter_PublicPath_SkipsAuthentication")
    void filter_PublicPath_SkipsAuthentication() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(tokenValidator, never()).validate(anyString());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_ActuatorPath_SkipsAuthentication")
    void filter_ActuatorPath_SkipsAuthentication() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/actuator/health").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(tokenValidator, never()).validate(anyString());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_WebhookPath_SkipsAuthentication")
    void filter_WebhookPath_SkipsAuthentication() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/webhooks/stripe").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(tokenValidator, never()).validate(anyString());
    }

    @Test
    @DisplayName("filter_ProtectedPathWithoutToken_ReturnsUnauthorized")
    void filter_ProtectedPathWithoutToken_ReturnsUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter_ProtectedPathWithInvalidToken_ReturnsUnauthorized")
    void filter_ProtectedPathWithInvalidToken_ReturnsUnauthorized() {
        // Arrange
        when(tokenValidator.validate(anyString()))
            .thenThrow(new JwtValidationException("Невалидный токен"));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(chain, never()).filter(any());
    }

    @Test
    @DisplayName("filter_ProtectedPathWithValidToken_AddsUserHeaders")
    void filter_ProtectedPathWithValidToken_AddsUserHeaders() {
        // Arrange
        JwtTokenValidator.TokenInfo tokenInfo = new JwtTokenValidator.TokenInfo(
            USER_ID, EMAIL, TENANT_ID, ROLES
        );
        when(tokenValidator.validate(VALID_TOKEN)).thenReturn(tokenInfo);

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_MalformedAuthorizationHeader_ReturnsUnauthorized")
    void filter_MalformedAuthorizationHeader_ReturnsUnauthorized() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/events")
            .header(HttpHeaders.AUTHORIZATION, "Basic credentials")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(tokenValidator, never()).validate(anyString());
    }

    @Test
    @DisplayName("getOrder_ReturnsNegative100")
    void getOrder_ReturnsNegative100() {
        // Act & Assert
        assertEquals(-100, filter.getOrder());
    }

    @Test
    @DisplayName("filter_PublicEventsPath_SkipsAuthentication")
    void filter_PublicEventsPath_SkipsAuthentication() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .get("/api/v1/events/public/upcoming").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(tokenValidator, never()).validate(anyString());
        verify(chain).filter(any());
    }

    @Test
    @DisplayName("filter_RegisterPath_SkipsAuthentication")
    void filter_RegisterPath_SkipsAuthentication() {
        // Arrange
        MockServerHttpRequest request = MockServerHttpRequest
            .post("/api/v1/auth/register").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        // Act
        filter.filter(exchange, chain).block();

        // Assert
        verify(tokenValidator, never()).validate(anyString());
    }
}
