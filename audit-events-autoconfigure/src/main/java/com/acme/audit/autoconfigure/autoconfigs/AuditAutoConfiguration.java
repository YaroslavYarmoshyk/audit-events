package com.acme.audit.autoconfigure.autoconfigs;

import com.acme.audit.Audit;
import com.acme.audit.AuditEventPublisher;
import com.acme.audit.autoconfigure.AuditEventDispatcher;
import com.acme.audit.autoconfigure.AuditProperties;
import com.acme.audit.autoconfigure.DefaultAuditEventPublisher;
import com.acme.audit.autoconfigure.constants.AuditAutoConfigurationConstants;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.spi.AuditEventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.data.domain.AuditorAware;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;

/**
 * Core auto-configuration. Active unless {@code framework.audit-events.enabled=false}.
 * Wires the publisher, the async dispatcher pipeline and the supporting beans, each
 * overridable via {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration(after = AuditStoreAutoConfiguration.class)
@ConditionalOnProperty(prefix = "framework.audit-events", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ObjectMapper auditObjectMapper() {
        return new ObjectMapper();
    }

    /**
     * Fallback auditor when neither the client nor Spring Security provides one.
     */
    @Bean
    @ConditionalOnMissingBean(AuditorAware.class)
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(AuditConstants.ANONYMOUS);
    }

    @Bean(name = "auditTaskExecutor")
    @ConditionalOnMissingBean(name = "auditTaskExecutor")
    public AsyncTaskExecutor auditTaskExecutor(AuditProperties properties,
                                               @Qualifier("auditTaskDecorator")
                                               ObjectProvider<TaskDecorator> taskDecorator) {
        AuditProperties.Async async = properties.getAsync();
        if (async.getMode() == AuditProperties.Async.Mode.VIRTUAL) {
            SimpleAsyncTaskExecutor executor =
                    new SimpleAsyncTaskExecutor(AuditAutoConfigurationConstants.THREAD_NAME_PREFIX);
            executor.setVirtualThreads(true);
            taskDecorator.ifAvailable(executor::setTaskDecorator);
            return executor;
        }
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(AuditAutoConfigurationConstants.THREAD_NAME_PREFIX);
        executor.setCorePoolSize(async.getPlatformPoolSize());
        executor.setMaxPoolSize(async.getPlatformPoolSize());
        taskDecorator.ifAvailable(executor::setTaskDecorator);
        executor.initialize();
        return executor;
    }

    /**
     * Carries the caller's context - OpenTelemetry trace scope and the SLF4J MDC - onto the async
     * persistence thread, so {@code AuditEventDispatcher} logs share the originating trace id.
     * Only active when Micrometer context-propagation is on the classpath (i.e. the app uses tracing);
     * without it the executor runs undecorated, exactly as before.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "io.micrometer.context.ContextSnapshotFactory")
    static class AuditContextPropagationConfiguration {

        @Bean(name = "auditTaskDecorator")
        @ConditionalOnMissingBean(name = "auditTaskDecorator")
        TaskDecorator auditTaskDecorator() {
            return new ContextPropagatingTaskDecorator();
        }
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventPublisher auditEventPublisher(ApplicationEventPublisher events,
                                                   AuditorAware<String> auditorAware,
                                                   AuditProperties properties,
                                                   ObjectMapper objectMapper) {
        return new DefaultAuditEventPublisher(events, auditorAware, properties.getZone(), objectMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuditEventDispatcher auditEventDispatcher(AuditEventStore store,
                                                     AsyncTaskExecutor auditTaskExecutor,
                                                     AuditProperties properties,
                                                     ObjectProvider<MeterRegistry> meterRegistry) {
        return new AuditEventDispatcher(store, auditTaskExecutor,
                properties.getAsync().isEnabled(), meterRegistry);
    }

    /**
     * Wires the static {@link Audit} facade to the active publisher.
     */
    @Bean
    AuditStaticRegistrar auditStaticRegistrar(AuditEventPublisher publisher) {
        return new AuditStaticRegistrar(publisher);
    }

    static final class AuditStaticRegistrar {
        AuditStaticRegistrar(AuditEventPublisher publisher) {
            Audit.setPublisher(publisher);
        }
    }
}
