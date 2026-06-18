package com.acme.audit.autoconfigure.mvc;

import java.util.Optional;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.autoconfigure.AuditProperties;
import com.acme.audit.autoconfigure.autoconfigs.AuditAutoConfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reuses Spring Security on the <strong>servlet</strong> stack: captures LOGIN/LOGOUT events and
 * provides a security-aware default {@link AuditorAware}. Active only when Spring Security is on
 * the classpath and the application is a servlet web app.
 *
 * <p>It is intentionally restricted to {@code type = SERVLET}: the auditor reads the thread-bound
 * {@link SecurityContextHolder} and the listener hooks servlet authentication events, neither of
 * which apply on the reactive stack. Reactive (WebFlux) apps get their equivalents from
 * {@code audit-events-autoconfigure-webflux} instead.
 *
 * <p>Ordered before {@link AuditAutoConfiguration} so this servlet {@code AuditorAware} wins over
 * that module's anonymous fallback.
 */
@AutoConfiguration(before = AuditAutoConfiguration.class)
@ConditionalOnClass(AuthenticationSuccessEvent.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "framework.audit-events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditSecurityAutoConfiguration {

    /** Resolves createdBy from the current authentication; falls back to anonymous. */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> securityAuditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return Optional.ofNullable(authentication)
                    .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
                    .map(Authentication::getName);
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditSecurityListener auditSecurityListener(AuditEventPublisher publisher, AuditProperties properties) {
        return new AuditSecurityListener(publisher,
                properties.getSecurity().isLoginEventsEnabled(),
                properties.getSecurity().isLogoutEventsEnabled());
    }
}
