package com.acme.audit.autoconfigure;

import javax.sql.DataSource;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.autoconfigure.autoconfigs.AuditAutoConfiguration;
import com.acme.audit.autoconfigure.autoconfigs.AuditDisabledAutoConfiguration;
import com.acme.audit.autoconfigure.autoconfigs.AuditStoreAutoConfiguration;
import com.acme.audit.autoconfigure.store.AuditDataSource;
import com.acme.audit.autoconfigure.store.JdbcAuditEventStore;
import com.acme.audit.autoconfigure.testsupport.TestDataSources;
import com.acme.audit.spi.AuditEventStore;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DisabledAndDataSourceResolutionTest {

    @Test
    void disabledInstallsNoOpPublisherAndNoStore() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AuditStoreAutoConfiguration.class,
                        AuditAutoConfiguration.class,
                        AuditDisabledAutoConfiguration.class))
                .withPropertyValues("framework.audit-events.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(AuditEventPublisher.class);
                    assertThat(context).doesNotHaveBean(AuditEventStore.class);
                    assertThat(context).doesNotHaveBean(AuditEventDispatcher.class);
                });
    }

    @Test
    void ambiguousDataSourcesWithoutQualifierFailFast() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AuditStoreAutoConfiguration.class, AuditAutoConfiguration.class))
                .withPropertyValues("framework.audit-events.storage.type=jdbc")
                .withUserConfiguration(TwoUnqualifiedDataSources.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void auditDataSourceQualifierIsSelectedAmongMany() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AuditStoreAutoConfiguration.class, AuditAutoConfiguration.class))
                .withPropertyValues("framework.audit-events.storage.type=jdbc", "framework.audit-events.storage.jdbc.schema-init=true")
                .withUserConfiguration(QualifiedDataSources.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(AuditEventStore.class)).isInstanceOf(JdbcAuditEventStore.class);
                });
    }

    @Test
    void explicitBeanNameIsHonoured() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        AuditStoreAutoConfiguration.class, AuditAutoConfiguration.class))
                .withPropertyValues("framework.audit-events.storage.type=jdbc",
                        "framework.audit-events.storage.jdbc.datasource-bean=reportingDs",
                        "framework.audit-events.storage.jdbc.schema-init=true")
                .withUserConfiguration(TwoUnqualifiedDataSources.class)
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    static class TwoUnqualifiedDataSources {
        @Bean DataSource primaryDs() { return TestDataSources.h2("primary"); }
        @Bean DataSource reportingDs() { return TestDataSources.h2("reporting"); }
    }

    @Configuration(proxyBeanMethods = false)
    static class QualifiedDataSources {
        @Bean DataSource primaryDs() { return TestDataSources.h2("primary2"); }
        @Bean @AuditDataSource DataSource auditDs() { return TestDataSources.h2("auditq"); }
    }
}
