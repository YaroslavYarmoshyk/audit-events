package com.acme.audit.autoconfigure.security;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.AuditPrincipalResolver;
import com.acme.audit.constants.AuditConstants;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

/**
 * Records a LOGOUT as a reactive logout completes, then delegates to the handler it wraps. The
 * reactive stack publishes no {@code LogoutSuccessEvent}, so reactive apps wire this decorator around
 * their own logout-success handler in the {@code SecurityWebFilterChain}:
 *
 * <pre>{@code
 * http.logout(l -> l.logoutSuccessHandler(
 *         new AuditServerLogoutSuccessHandler(
 *                 new RedirectServerLogoutSuccessHandler(),
 *                 auditEventPublisher, auditPrincipalResolver)));
 * }</pre>
 *
 * <p>When logout is invoked without an authenticated user the callback receives a {@code null}
 * authentication; nothing is recorded then, but the wrapped handler still runs.
 */
@RequiredArgsConstructor
public class AuditServerLogoutSuccessHandler implements ServerLogoutSuccessHandler {

    private final ServerLogoutSuccessHandler delegate;
    private final AuditEventPublisher publisher;
    private final AuditPrincipalResolver principalResolver;

    @NonNull
    @Override
    public Mono<Void> onLogoutSuccess(WebFilterExchange exchange, @Nullable Authentication authentication) {
        if (authentication != null) {
            publisher.publishAs(AuditPrincipals.resolve(authentication, principalResolver), AuditConstants.LOGOUT, null);
        }
        return delegate.onLogoutSuccess(exchange, authentication);
    }
}
