package com.acme.audit.autoconfigure.autoconfigs;

import java.time.Clock;

import com.acme.audit.autoconfigure.AuditProperties;
import com.acme.audit.autoconfigure.AuditRetentionJob;
import com.acme.audit.spi.AuditEventStore;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Schedules the retention purge. Off by default; enable with
 * {@code framework.audit-events.retention.enabled=true}.
 */
@AutoConfiguration(after = {AuditAutoConfiguration.class, AuditStoreAutoConfiguration.class})
@ConditionalOnBean(AuditEventStore.class)
@ConditionalOnProperty(prefix = "framework.audit-events.retention", name = "enabled", havingValue = "true")
@EnableScheduling
public class AuditRetentionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditRetentionJob auditRetentionJob(AuditEventStore store, AuditProperties properties, Clock clock) {
        return new AuditRetentionJob(store, properties.getRetention(), clock);
    }
}
