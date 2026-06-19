package com.acme.audit.autoconfigure.autoconfigs;

import java.util.Optional;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.AuditPrincipalResolver;
import com.acme.audit.autoconfigure.AuditProperties;
import com.acme.audit.autoconfigure.OAuth2AuditPrincipalResolver;
import com.acme.audit.autoconfigure.security.AuditSecurityListener;

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
 * provides a security-aware default {@link AuditorAware}. Active only when Spring Security is on the
 * classpath and the application is a servlet web app.
 *
 * <p>It is intentionally restricted to {@code type = SERVLET}: the auditor reads the thread-bound
 * {@link SecurityContextHolder} and the listener hooks servlet authentication events, neither of
 * which apply on the reactive stack. Reactive (WebFlux) apps get their equivalents from
 * {@link ReactiveAuditSecurityAutoConfiguration} instead.
 *
 * <p>Ordered before {@link AuditAutoConfiguration} so this servlet {@code AuditorAware} wins over
 * that module's anonymous fallback.
 */
@AutoConfiguration(before = AuditAutoConfiguration.class)
@ConditionalOnClass(AuthenticationSuccessEvent.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "framework.audit-events", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditSecurityAutoConfiguration {

    /**
     * Default resolver for OAuth2/OIDC logins ({@code preferred_username}). Throws for any other
     * authentication, so non-OAuth2 apps must declare their own {@link AuditPrincipalResolver}, which
     * replaces this one.
     */
    @Bean
    @ConditionalOnMissingBean
    public AuditPrincipalResolver auditPrincipalResolver() {
        return new OAuth2AuditPrincipalResolver();
    }

    /** Resolves createdBy from the current authentication; falls back to anonymous. */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> securityAuditorAware(AuditPrincipalResolver principalResolver) {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return Optional.ofNullable(authentication)
                    .filter(auth -> !(auth instanceof AnonymousAuthenticationToken))
                    .map(principalResolver::resolve)
                    .filter(name -> !name.isBlank());
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditSecurityListener auditSecurityListener(AuditEventPublisher publisher,
                                                       AuditProperties properties,
                                                       AuditPrincipalResolver principalResolver) {
        return new AuditSecurityListener(publisher,
                properties.getSecurity().isLoginEventsEnabled(),
                properties.getSecurity().isLogoutEventsEnabled(),
                principalResolver);
    }
}
