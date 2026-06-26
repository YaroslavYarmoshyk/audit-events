package com.acme.audit.autoconfigure;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.sql.DataSource;

import com.acme.audit.AuditEvent;
import com.acme.audit.autoconfigure.autoconfigs.AuditAutoConfiguration;
import com.acme.audit.autoconfigure.autoconfigs.AuditStoreAutoConfiguration;
import com.acme.audit.autoconfigure.store.JdbcAuditEventStore;
import com.acme.audit.autoconfigure.testsupport.TestDataSources;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.spi.AuditEventStore;
import com.acme.audit.spi.AuditSearchCriteria;
import com.acme.audit.testsupport.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JDBC store and retention purging")
class JdbcStoreAndRetentionTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AuditStoreAutoConfiguration.class,
                    AuditAutoConfiguration.class))
            .withUserConfiguration(DataSourceConfig.class)
            .withPropertyValues(
                    "framework.audit-events.async.enabled=false",
                    "framework.audit-events.storage.type=jdbc",
                    "framework.audit-events.storage.jdbc.table-name=audit_events",
                    "framework.audit-events.storage.jdbc.schema-init=true");

    @Configuration(proxyBeanMethods = false)
    static class DataSourceConfig {
        @Bean
        DataSource dataSource() {
            return TestDataSources.h2("auditdb");
        }
    }

    @Test
    @DisplayName("JDBC store is selected and persists and queries events")
    void jdbcStoreIsSelectedAndPersistsAndQueries() {
        runner.run(context -> {
            assertThat(context.getBean(AuditEventStore.class)).isInstanceOf(JdbcAuditEventStore.class);
            AuditEventStore store = context.getBean(AuditEventStore.class);

            store.save(AuditEvent.builder().type(AuditConstants.LOGIN).createdBy(TestData.ALICE).build());
            store.save(AuditEvent.builder().type(AuditConstants.LOGOUT).createdBy(TestData.BOB).build());

            var byType = store.search(
                    new AuditSearchCriteria(java.util.Set.of(AuditConstants.LOGIN), null, null, null),
                    PageRequest.of(0, 10));
            assertThat(byType.getTotalElements()).isEqualTo(1);
            assertThat(byType.getContent().getFirst().createdBy()).isEqualTo(TestData.ALICE);

            var byCreator = store.search(
                    new AuditSearchCriteria(null, java.util.Set.of(TestData.BOB), null, null),
                    PageRequest.of(0, 10));
            assertThat(byCreator.getContent().getFirst().type()).isEqualTo(AuditConstants.LOGOUT);
        });
    }

    @Test
    @DisplayName("Retention job deletes events older than the max age in batches")
    void retentionDeletesOldEventsInBatches() {
        runner.run(context -> {
            DataSource ds = context.getBean(DataSource.class);
            JdbcAuditEventStore store = (JdbcAuditEventStore) context.getBean(AuditEventStore.class);

            LocalDateTime old = LocalDateTime.now().minusDays(3650);
            store.save(AuditEvent.builder().type("OLD").createdAt(old).build());
            store.save(AuditEvent.builder().type("NEW").createdAt(LocalDateTime.now()).build());

            AuditProperties.Retention retention = new AuditProperties.Retention();
            retention.setMaxAge(java.time.Period.ofYears(7));
            retention.setBatchSize(100);
            AuditRetentionJob job = new AuditRetentionJob(store, retention, ZoneOffset.UTC);

            job.purge();

            var remaining = store.search(AuditSearchCriteria.all(), PageRequest.of(0, 10));
            assertThat(remaining.getContent()).extracting(AuditEvent::type).containsExactly("NEW");
            assertThat(ds).isNotNull();
        });
    }
}
