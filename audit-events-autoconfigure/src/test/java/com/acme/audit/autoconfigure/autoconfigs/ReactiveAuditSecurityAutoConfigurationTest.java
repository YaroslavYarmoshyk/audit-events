package com.acme.audit.autoconfigure.autoconfigs;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.AuditPrincipalResolver;
import com.acme.audit.NoOpAuditEventPublisher;
import com.acme.audit.autoconfigure.OAuth2AuditPrincipalResolver;
import com.acme.audit.autoconfigure.security.ReactiveAuditUserWebFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Reactive audit security auto-configuration")
class ReactiveAuditSecurityAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner reactiveRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ReactiveAuditSecurityAutoConfiguration.class))
            .withUserConfiguration(StubPublisherConfig.class);

    @Test
    @DisplayName("Reactive stack wires the reactive audit beans")
    void reactiveStackWiresReactiveAuditBeans() {
        reactiveRunner.run(context -> {
            assertThat(context).hasSingleBean(ReactiveAuditUserWebFilter.class);
            assertThat(context).hasSingleBean(AuditorAware.class);
            assertThat(context).getBean(AuditPrincipalResolver.class)
                    .isInstanceOf(OAuth2AuditPrincipalResolver.class);
            assertThat(context).hasSingleBean(
                    ReactiveAuditSecurityAutoConfiguration.ReactiveAuditUserContextRegistrar.class);
        });
    }

    @Test
    @DisplayName("A custom principal resolver overrides the OAuth2 default")
    void customPrincipalResolverOverridesDefault() {
        reactiveRunner.withUserConfiguration(CustomResolverConfig.class).run(context -> {
            assertThat(context).hasSingleBean(AuditPrincipalResolver.class);
            assertThat(context).doesNotHaveBean(OAuth2AuditPrincipalResolver.class);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Reactive AuditorAware is empty when no user is present")
    void reactiveAuditorAwareIsEmptyWithoutAUser() {
        reactiveRunner.run(context -> {
            AuditorAware<String> auditor = context.getBean(AuditorAware.class);
            assertThat(auditor.getCurrentAuditor()).isEmpty();
        });
    }

    @Test
    @DisplayName("Disabled master switch backs off the reactive beans")
    void disabledMasterSwitchBacksOff() {
        reactiveRunner.withPropertyValues("framework.audit-events.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ReactiveAuditUserWebFilter.class));
    }

    @Test
    @DisplayName("A non-reactive application does not activate the reactive beans")
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

    @Configuration(proxyBeanMethods = false)
    static class CustomResolverConfig {
        @Bean
        AuditPrincipalResolver auditPrincipalResolver() {
            return authentication -> "custom-" + authentication.getName();
        }
    }
}
