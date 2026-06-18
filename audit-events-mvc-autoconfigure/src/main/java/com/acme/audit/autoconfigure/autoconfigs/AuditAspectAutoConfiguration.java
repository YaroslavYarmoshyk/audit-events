package com.acme.audit.autoconfigure.autoconfigs;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.autoconfigure.AuditedAspect;
import org.aspectj.lang.annotation.Aspect;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Enables the {@link com.acme.audit.Audited @Audited} aspect when AOP is on the
 * classpath and a publisher exists.
 */
@AutoConfiguration(after = AuditAutoConfiguration.class)
@ConditionalOnClass(Aspect.class)
@ConditionalOnProperty(prefix = "framework.audit-events", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableAspectJAutoProxy
public class AuditAspectAutoConfiguration {

    @Bean
    @ConditionalOnBean(AuditEventPublisher.class)
    @ConditionalOnMissingBean
    public AuditedAspect auditedAspect(AuditEventPublisher publisher) {
        return new AuditedAspect(publisher);
    }
}
