package com.acme.audit.autoconfigure;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.constants.AuditConstants;
import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;

/**
 * Captures LOGIN/LOGOUT audit events by listening to Spring Security's own application
 * events - no custom authentication handlers required.
 *
 * <p>The acting principal is taken from the event itself, not the ambient
 * {@code SecurityContextHolder}: these events fire before the SecurityContext is established
 * (login) or after it is cleared (logout), so the holder is unreliable here.
 *
 * <p>LOGIN is sourced from {@code InteractiveAuthenticationSuccessEvent} - one record per
 * interactive login (form/OAuth2 login, remember-me). Stateless flows (JWT/bearer) have no
 * interactive login moment to hook, so applications using them must record LOGIN/LOGOUT
 * themselves where the login actually happens (see the README).
 */
@RequiredArgsConstructor
public class AuditSecurityListener {
    private final AuditEventPublisher publisher;
    private final boolean loginEnabled;
    private final boolean logoutEnabled;

    /** Interactive login (form/OAuth2 login, remember-me); fired once after the SecurityContext is set. */
    @EventListener
    public void onInteractiveAuthenticationSuccess(InteractiveAuthenticationSuccessEvent event) {
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
