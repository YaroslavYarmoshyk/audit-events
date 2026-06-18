package com.acme.audit.autoconfigure.webflux;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.NoOpAuditEventPublisher;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveAuditSecurityAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner reactiveRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveAuditSecurityAutoConfiguration.class))
            .withUserConfiguration(StubPublisherConfig.class);

    @Test
    void reactiveStackWiresReactiveAuditBeans() {
        reactiveRunner.run(context -> {
            assertThat(context).hasSingleBean(ReactiveAuditUserWebFilter.class);
            assertThat(context).hasSingleBean(ReactiveAuditSecurityListener.class);
            assertThat(context).hasSingleBean(AuditorAware.class);
            assertThat(context).hasSingleBean(
                    ReactiveAuditSecurityAutoConfiguration.ReactiveAuditUserContextRegistrar.class);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void reactiveAuditorAwareIsEmptyWithoutAUser() {
        reactiveRunner.run(context -> {
            AuditorAware<String> auditor = context.getBean(AuditorAware.class);
            assertThat(auditor.getCurrentAuditor()).isEmpty();
        });
    }

    @Test
    void disabledMasterSwitchBacksOff() {
        reactiveRunner.withPropertyValues("framework.audit-events.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ReactiveAuditUserWebFilter.class));
    }

    @Test
    void nonReactiveApplicationDoesNotActivate() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ReactiveAuditSecurityAutoConfiguration.class))
                .withUserConfiguration(StubPublisherConfig.class)
                .run(context -> assertThat(context).doesNotHaveBean(ReactiveAuditUserWebFilter.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class StubPublisherConfig {
        @Bean
        AuditEventPublisher auditEventPublisher() {
            return new NoOpAuditEventPublisher();
        }
    }
}
