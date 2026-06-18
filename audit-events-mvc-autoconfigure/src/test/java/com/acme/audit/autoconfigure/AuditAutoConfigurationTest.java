package com.acme.audit.autoconfigure;

import java.time.ZoneId;
import java.util.Optional;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.autoconfigure.autoconfigs.AuditAutoConfiguration;
import com.acme.audit.autoconfigure.autoconfigs.AuditStoreAutoConfiguration;
import com.acme.audit.autoconfigure.store.InMemoryAuditEventStore;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import com.acme.audit.spi.AuditEventStore;
import com.acme.audit.spi.AuditSearchCriteria;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AuditAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AuditStoreAutoConfiguration.class,
                    AuditAutoConfiguration.class))
            // synchronous persistence for deterministic assertions
            .withPropertyValues("framework.audit-events.async.enabled=false");

    @Test
    void defaultsToInMemoryStoreAndStartsWithoutDatabase() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AuditEventStore.class);
            assertThat(context.getBean(AuditEventStore.class)).isInstanceOf(InMemoryAuditEventStore.class);
            assertThat(context).hasSingleBean(AuditEventPublisher.class);
        });
    }

    @Test
    void publishedEventIsPersistedWithResolvedAuditFields() {
        runner.run(context -> {
            AuditEventPublisher publisher = context.getBean(AuditEventPublisher.class);
            publisher.publish(AuditConstants.LOGIN, "hello");

            AuditEventStore store = context.getBean(AuditEventStore.class);
            var page = store.search(AuditSearchCriteria.all(), PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().getFirst().type()).isEqualTo(AuditConstants.LOGIN);
            assertThat(page.getContent().getFirst().metadata()).isEqualTo("hello");
            assertThat(page.getContent().getFirst().createdBy()).isEqualTo(AuditConstants.ANONYMOUS);
        });
    }

    @Test
    void mapMetadataIsSerializedToJson() {
        runner.run(context -> {
            context.getBean(AuditEventPublisher.class)
                    .publish(TestData.ORDER_CREATED, java.util.Map.of("order", 123));

            var page = context.getBean(AuditEventStore.class)
                    .search(AuditSearchCriteria.all(), PageRequest.of(0, 10));
            assertThat(page.getContent().getFirst().metadata()).isEqualTo("{\"order\":123}");
        });
    }

    @Test
    void existingAuditorAwareBeanIsReused() {
        runner.withUserConfiguration(CustomAuditorConfig.class).run(context -> {
            assertThat(context).hasSingleBean(AuditorAware.class);
            context.getBean(AuditEventPublisher.class).publish("X");

            var page = context.getBean(AuditEventStore.class)
                    .search(AuditSearchCriteria.all(), PageRequest.of(0, 10));
            assertThat(page.getContent().getFirst().createdBy()).isEqualTo(TestData.ALICE);
        });
    }

    @Test
    void retentionZoneDefaultsToTorontoAndBindsFromProperty() {
        runner.run(context -> assertThat(context.getBean(AuditProperties.class).getZone())
                .isEqualTo(ZoneId.of("America/Toronto")));

        runner.withPropertyValues("framework.audit-events.zone=Europe/Paris")
                .run(context -> assertThat(context.getBean(AuditProperties.class).getZone())
                        .isEqualTo(ZoneId.of("Europe/Paris")));
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomAuditorConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of(TestData.ALICE);
        }
    }
}
