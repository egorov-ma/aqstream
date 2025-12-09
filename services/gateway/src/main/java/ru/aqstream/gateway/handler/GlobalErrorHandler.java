package ru.aqstream.gateway.handler;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import ru.aqstream.common.api.ErrorResponse;

/**
 * Глобальный обработчик ошибок для Gateway.
 * Форматирует все ошибки в единый формат ErrorResponse.
 */
@Component
@Order(-2) // Выше приоритет чем DefaultErrorWebExceptionHandler
public class GlobalErrorHandler extends AbstractErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    public GlobalErrorHandler(
        ErrorAttributes errorAttributes,
        WebProperties webProperties,
        ApplicationContext applicationContext,
        ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        setMessageWriters(serverCodecConfigurer.getWriters());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Map<String, Object> errorAttributes = getErrorAttributes(
            request, ErrorAttributeOptions.defaults()
        );

        Throwable error = getError(request);
        HttpStatus status = determineHttpStatus(errorAttributes);
        String code = determineErrorCode(status);
        String message = determineMessage(status);

        log.error("Ошибка Gateway: path={}, status={}, error={}",
            request.path(), status.value(), error.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(code, message, Map.of(
            "path", request.path()
        ));

        return ServerResponse
            .status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorResponse));
    }

    /**
     * Определяет HTTP статус из атрибутов ошибки.
     */
    private HttpStatus determineHttpStatus(Map<String, Object> errorAttributes) {
        Object statusObj = errorAttributes.get("status");
        if (statusObj instanceof Integer statusCode) {
            HttpStatus status = HttpStatus.resolve(statusCode);
            return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    /**
     * Определяет код ошибки для клиента.
     */
    private String determineErrorCode(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "not_found";
            case UNAUTHORIZED -> "unauthorized";
            case FORBIDDEN -> "forbidden";
            case BAD_REQUEST -> "bad_request";
            case TOO_MANY_REQUESTS -> "rate_limit_exceeded";
            case SERVICE_UNAVAILABLE -> "service_unavailable";
            case BAD_GATEWAY, GATEWAY_TIMEOUT -> "downstream_error";
            default -> "internal_error";
        };
    }

    /**
     * Определяет сообщение об ошибке для клиента.
     */
    private String determineMessage(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Ресурс не найден";
            case UNAUTHORIZED -> "Требуется авторизация";
            case FORBIDDEN -> "Доступ запрещён";
            case BAD_REQUEST -> "Некорректный запрос";
            case TOO_MANY_REQUESTS -> "Превышен лимит запросов";
            case SERVICE_UNAVAILABLE -> "Сервис временно недоступен";
            case BAD_GATEWAY, GATEWAY_TIMEOUT -> "Ошибка связи с сервисом";
            default -> "Внутренняя ошибка сервера";
        };
    }
}
