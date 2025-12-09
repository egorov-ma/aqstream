package ru.aqstream.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.security.UserPrincipal;

/**
 * Фильтр для установки TenantContext и MDC из аутентифицированного пользователя.
 * Должен выполняться после Spring Security фильтров.
 *
 * <p>Извлекает данные из UserPrincipal и устанавливает:</p>
 * <ul>
 *   <li>TenantContext — для multi-tenancy</li>
 *   <li>MDC (tenantId, userId) — для логирования</li>
 * </ul>
 */
@Component
@Order(100) // После Spring Security фильтров
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);
    private static final String MDC_TENANT_ID = "tenantId";
    private static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            extractUserPrincipal().ifPresent(principal -> {
                // Устанавливаем TenantContext
                if (principal.tenantId() != null) {
                    TenantContext.setTenantId(principal.tenantId());
                    MDC.put(MDC_TENANT_ID, principal.tenantId().toString());
                }

                // Добавляем userId в MDC для логирования
                if (principal.userId() != null) {
                    MDC.put(MDC_USER_ID, principal.userId().toString());
                }

                log.trace("Контекст установлен: tenantId={}, userId={}",
                    principal.tenantId(), principal.userId());
            });

            filterChain.doFilter(request, response);

        } finally {
            // Обязательно очищаем контекст после обработки запроса
            TenantContext.clear();
            MDC.remove(MDC_TENANT_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    /**
     * Извлекает UserPrincipal из SecurityContext.
     *
     * @return Optional с UserPrincipal или пустой Optional
     */
    private Optional<UserPrincipal> extractUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal);
        }

        return Optional.empty();
    }
}
