package com.acme.audit.autoconfigure;

import com.acme.audit.Audited;
import com.acme.audit.autoconfigure.autoconfigs.AuditAspectAutoConfiguration;
import com.acme.audit.testsupport.TestData;
import com.acme.audit.autoconfigure.autoconfigs.AuditAutoConfiguration;
import com.acme.audit.autoconfigure.autoconfigs.AuditStoreAutoConfiguration;
import com.acme.audit.spi.AuditEventStore;
import com.acme.audit.spi.AuditSearchCriteria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("@Audited aspect")
class AuditedAspectTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AuditStoreAutoConfiguration.class,
                    AuditAutoConfiguration.class,
                    AuditAspectAutoConfiguration.class))
            .withPropertyValues("framework.audit-events.async.enabled=false")
            .withUserConfiguration(ServiceConfig.class);

    @Test
    @DisplayName("Annotated method records an event with SpEL-evaluated metadata")
    void annotatedMethodRecordsEventWithSpelMetadata() {
        runner.run(context -> {
            context.getBean(OrderService.class).cancel(new Order("123"));

            var page = context.getBean(AuditEventStore.class)
                    .search(AuditSearchCriteria.all(), PageRequest.of(0, 10));
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().getFirst().type()).isEqualTo(TestData.ORDER_CANCELLED);
            assertThat(page.getContent().getFirst().metadata()).isEqualTo("123");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ServiceConfig {
        @Bean
        OrderService orderService() {
            return new OrderService();
        }
    }

    @Service
    static class OrderService {
        @Audited(type = TestData.ORDER_CANCELLED, metadata = "#order.id")
        public void cancel(Order order) {
            // business logic
        }
    }

    record Order(String id) {
    }
}
