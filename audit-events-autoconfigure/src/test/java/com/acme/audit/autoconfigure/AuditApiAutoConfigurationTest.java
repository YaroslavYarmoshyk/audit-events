package com.acme.audit.autoconfigure;

import java.time.LocalDate;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.autoconfigure.api.AuditQueryController;
import com.acme.audit.autoconfigure.autoconfigs.AuditApiAutoConfiguration;
import com.acme.audit.autoconfigure.autoconfigs.AuditAutoConfiguration;
import com.acme.audit.autoconfigure.autoconfigs.AuditStoreAutoConfiguration;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AuditApiAutoConfigurationTest {
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AuditStoreAutoConfiguration.class,
                    AuditAutoConfiguration.class,
                    AuditApiAutoConfiguration.class))
            .withPropertyValues("framework.audit-events.async.enabled=false");

    @Test
    void apiIsDisabledByDefault() {
        runner.run(context -> assertThat(context).doesNotHaveBean(AuditQueryController.class));
    }

    @Test
    void apiIsExposedWhenEnabledAndReturnsFilteredResults() {
        runner.withPropertyValues("framework.audit-events.api.enabled=true").run(context -> {
            assertThat(context).hasSingleBean(AuditQueryController.class);

            AuditEventPublisher publisher = context.getBean(AuditEventPublisher.class);
            publisher.publish(AuditConstants.LOGIN, "x");
            publisher.publish(TestData.ORDER_CREATED, "y");

            AuditQueryController controller = context.getBean(AuditQueryController.class);
            var loginOnly = controller.search(java.util.Set.of(AuditConstants.LOGIN), null, null, null, 0, 20);
            assertThat(loginOnly.getMetadata().totalElements()).isEqualTo(1);
            assertThat(loginOnly.getContent().getFirst().type()).isEqualTo(AuditConstants.LOGIN);

            var all = controller.search(null, null, null, null, 0, 20);
            assertThat(all.getMetadata().totalElements()).isEqualTo(2);
        });
    }

    @Test
    void dateRangeIsInterpretedAsWholeDaysInclusive() {
        runner.withPropertyValues("framework.audit-events.api.enabled=true").run(context -> {
            context.getBean(AuditEventPublisher.class).publish(AuditConstants.LOGIN, "x");
            AuditQueryController controller = context.getBean(AuditQueryController.class);
            LocalDate today = LocalDate.now();

            var inRange = controller.search(null, null, today.minusDays(1), today.plusDays(1), 0, 20);
            assertThat(inRange.getMetadata().totalElements()).isEqualTo(1);

            var pastWindow = controller.search(null, null, today.minusDays(10), today.minusDays(5), 0, 20);
            assertThat(pastWindow.getMetadata().totalElements()).isZero();
        });
    }
}
