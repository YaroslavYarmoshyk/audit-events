package com.acme.audit.autoconfigure.webflux;

import java.util.Optional;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.autoconfigure.AuditProperties;
import com.acme.audit.autoconfigure.autoconfigs.AuditAutoConfiguration;
import io.micrometer.context.ContextRegistry;
import io.micrometer.context.ThreadLocalAccessor;
import reactor.core.publisher.Hooks;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilter;

/**
 * Reactive (WebFlux) counterpart to the servlet {@code AuditSecurityAutoConfiguration}. Provides
 * automatic user attribution and LOGIN/LOGOUT capture for reactive applications.
 *
 * <p>Active only when the application is a reactive web app with Spring Security, WebFlux and
 * Micrometer context-propagation on the classpath. It contributes:
 * <ul>
 *   <li>a {@link ReactiveAuditUserWebFilter} that lifts the authenticated user into the Reactor
 *       {@code Context} per request;</li>
 *   <li>a {@link ReactiveAuditUserAccessor} registered with the global {@link ContextRegistry} (plus
 *       Reactor automatic context propagation), so the user is restored to a thread-local;</li>
 *   <li>a synchronous {@link AuditorAware} reading that thread-local, which the publisher uses to
 *       attribute {@code publish(...)} calls - mirroring the servlet behaviour;</li>
 *   <li>a {@link ReactiveAuditSecurityListener} for LOGIN/LOGOUT events.</li>
 * </ul>
 *
 * <p>Ordered before {@link AuditAutoConfiguration} so this reactive {@code AuditorAware} wins over
 * that module's anonymous fallback.
 */
@AutoConfiguration(before = AuditAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({ReactiveSecurityContextHolder.class, WebFilter.class, ThreadLocalAccessor.class})
@ConditionalOnProperty(prefix = "framework.audit-events", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuditProperties.class)
public class ReactiveAuditSecurityAutoConfiguration {

    /**
     * Registers the {@link ReactiveAuditUserAccessor} and enables Reactor automatic context
     * propagation, so the reactive user flows into the thread-local read by {@link #reactiveAuditorAware()}.
     */
    @Bean
    @ConditionalOnMissingBean
    ReactiveAuditUserContextRegistrar reactiveAuditUserContextRegistrar() {
        return new ReactiveAuditUserContextRegistrar();
    }

    /** Resolves createdBy from the propagated reactive user; falls back to anonymous when absent. */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> reactiveAuditorAware() {
        return () -> Optional.ofNullable(ReactiveAuditUserAccessor.current());
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveAuditUserWebFilter reactiveAuditUserWebFilter() {
        return new ReactiveAuditUserWebFilter();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReactiveAuditSecurityListener reactiveAuditSecurityListener(AuditEventPublisher publisher,
                                                                       AuditProperties properties) {
        return new ReactiveAuditSecurityListener(publisher,
                properties.getSecurity().isLoginEventsEnabled(),
                properties.getSecurity().isLogoutEventsEnabled());
    }

    /** One-shot registration of the thread-local accessor and Reactor context propagation. */
    static final class ReactiveAuditUserContextRegistrar {
        ReactiveAuditUserContextRegistrar() {
            ContextRegistry.getInstance().registerThreadLocalAccessor(new ReactiveAuditUserAccessor());
            Hooks.enableAutomaticContextPropagation();
        }
    }
}
