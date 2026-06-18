package com.acme.audit.autoconfigure.autoconfigs;

import java.time.Clock;

import com.acme.audit.autoconfigure.api.AuditQueryController;
import com.acme.audit.autoconfigure.api.AuditQueryService;
import com.acme.audit.spi.AuditEventStore;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the read-side REST API. Disabled by default; enable with
 * {@code framework.audit-events.api.enabled=true}. Authorization is the application's
 * responsibility via a {@code SecurityFilterChain} matching
 * {@code framework.audit-events.api.base-path}.
 */
@AutoConfiguration(after = {AuditAutoConfiguration.class, AuditStoreAutoConfiguration.class})
@ConditionalOnWebApplication
@ConditionalOnClass(RestController.class)
@ConditionalOnBean(AuditEventStore.class)
@ConditionalOnProperty(prefix = "framework.audit-events.api", name = "enabled", havingValue = "true")
public class AuditApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditQueryService auditQueryService(AuditEventStore store, Clock clock) {
        return new AuditQueryService(store, clock.getZone());
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditQueryController auditQueryController(AuditQueryService service) {
        return new AuditQueryController(service);
    }
}
