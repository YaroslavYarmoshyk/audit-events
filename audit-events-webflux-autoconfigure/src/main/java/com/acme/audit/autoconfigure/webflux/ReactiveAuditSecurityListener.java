package com.acme.audit.autoconfigure.webflux;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.constants.AuditConstants;
import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;

/**
 * Captures LOGIN/LOGOUT on the reactive stack by listening to Spring Security application events.
 *
 * <p>WebFlux has no {@code InteractiveAuthenticationSuccessEvent}, so LOGIN is sourced from
 * {@code AuthenticationSuccessEvent}. That fires once per interactive login in a login-based app,
 * but per request in a stateless (JWT/bearer) resource server - such apps should disable
 * {@code framework.audit-events.security.login-events-enabled} and record LOGIN where the login
 * truly happens (see the README).
 *
 * <p>These events are only delivered when the application's reactive authentication manager actually
 * publishes them; otherwise record LOGIN/LOGOUT explicitly via {@code publishAs(...)}.
 */
@RequiredArgsConstructor
public class ReactiveAuditSecurityListener {
    private final AuditEventPublisher publisher;
    private final boolean loginEnabled;
    private final boolean logoutEnabled;

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (loginEnabled) {
            publisher.publishAs(resolveUser(event), AuditConstants.LOGIN, null);
        }
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        if (logoutEnabled) {
            publisher.publishAs(resolveUser(event), AuditConstants.LOGOUT, null);
        }
    }

    /** Principal carried by the event, falling back to anonymous when absent. */
    private static String resolveUser(AbstractAuthenticationEvent event) {
        Authentication authentication = event.getAuthentication();
        String name = !(authentication instanceof AnonymousAuthenticationToken) ? authentication.getName() : null;
        return (name != null && !name.isBlank()) ? name : AuditConstants.ANONYMOUS;
    }
}
