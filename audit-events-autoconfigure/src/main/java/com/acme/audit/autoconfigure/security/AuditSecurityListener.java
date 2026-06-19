package com.acme.audit.autoconfigure.security;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.AuditPrincipalResolver;
import com.acme.audit.constants.AuditConstants;
import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;

/**
 * Captures LOGIN/LOGOUT on the <strong>servlet</strong> stack by listening to Spring Security's own
 * application events - no custom handlers required, because the servlet stack publishes them.
 *
 * <p>The acting principal is taken from the event itself, not the ambient {@code SecurityContextHolder}:
 * these events fire before the SecurityContext is established (login) or after it is cleared (logout).
 *
 * <p>LOGIN is sourced from {@code InteractiveAuthenticationSuccessEvent} - one record per interactive
 * login (form/OAuth2 login, remember-me). Stateless flows (JWT/bearer) have no interactive login
 * moment to hook, so such applications must record LOGIN/LOGOUT themselves (see the README).
 *
 * <p>The reactive (WebFlux) stack publishes no such events; reactive apps wire the
 * {@link AuditServerAuthenticationSuccessHandler} / {@link AuditServerLogoutSuccessHandler} instead.
 */
@RequiredArgsConstructor
public class AuditSecurityListener {
    private final AuditEventPublisher publisher;
    private final boolean loginEnabled;
    private final boolean logoutEnabled;
    private final AuditPrincipalResolver principalResolver;

    /** Interactive login (form/OAuth2 login, remember-me); fired once after the SecurityContext is set. */
    @EventListener
    public void onInteractiveAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
        if (loginEnabled) {
            publisher.publishAs(AuditPrincipals.resolve(event.getAuthentication(), principalResolver),
                    AuditConstants.LOGIN, null);
        }
    }

    @EventListener
    public void onLogoutSuccess(LogoutSuccessEvent event) {
        if (logoutEnabled) {
            publisher.publishAs(AuditPrincipals.resolve(event.getAuthentication(), principalResolver),
                    AuditConstants.LOGOUT, null);
        }
    }
}
