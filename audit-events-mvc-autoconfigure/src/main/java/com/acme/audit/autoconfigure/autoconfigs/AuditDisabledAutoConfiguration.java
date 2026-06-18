package com.acme.audit.autoconfigure.autoconfigs;

import com.acme.audit.Audit;
import com.acme.audit.AuditEventPublisher;
import com.acme.audit.NoOpAuditEventPublisher;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Installed when {@code framework.audit-events.enabled=false}: a no-op publisher so application code that
 * injects {@link AuditEventPublisher} (or uses the static {@link Audit} facade) keeps
 * working without any storage, listeners or async machinery.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework.audit-events", name = "enabled", havingValue = "false")
public class AuditDisabledAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditEventPublisher auditEventPublisher() {
        NoOpAuditEventPublisher publisher = new NoOpAuditEventPublisher();
        Audit.setPublisher(publisher);
        return publisher;
    }
}
