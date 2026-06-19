package com.acme.audit.autoconfigure.security;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.AuditPrincipalResolver;
import com.acme.audit.constants.AuditConstants;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Mono;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;

/**
 * Records a LOGIN as a reactive sign-in completes, then delegates to the handler it wraps. Reactive
 * Spring Security publishes no authentication application events, so reactive apps record LOGIN by
 * wiring this decorator around their own success handler in the {@code SecurityWebFilterChain}:
 *
 * <pre>{@code
 * http.oauth2Login(o -> o.authenticationSuccessHandler(
 *         new AuditServerAuthenticationSuccessHandler(
 *                 new RedirectServerAuthenticationSuccessHandler("/"),
 *                 auditEventPublisher, auditPrincipalResolver)));
 * }</pre>
 *
 * <p>The acting principal is taken from the {@link Authentication} passed to the callback, and the
 * wrapped handler runs unchanged afterwards (its redirect/response behaviour is preserved).
 */
@RequiredArgsConstructor
public class AuditServerAuthenticationSuccessHandler implements ServerAuthenticationSuccessHandler {

    private final ServerAuthenticationSuccessHandler delegate;
    private final AuditEventPublisher publisher;
    private final AuditPrincipalResolver principalResolver;

    @NonNull
    @Override
    public Mono<Void> onAuthenticationSuccess(WebFilterExchange exchange, Authentication authentication) {
        publisher.publishAs(AuditPrincipals.resolve(authentication, principalResolver), AuditConstants.LOGIN, null);
        return delegate.onAuthenticationSuccess(exchange, authentication);
    }
}
