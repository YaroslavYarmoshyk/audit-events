package com.acme.audit;

import java.time.Instant;
import java.util.UUID;

import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventTest {

    @Test
    void builderAppliesSensibleDefaults() {
        AuditEvent event = AuditEvent.builder().type(AuditConstants.LOGIN).build();

        assertThat(event.id()).isNotNull();
        assertThat(event.type()).isEqualTo(AuditConstants.LOGIN);
        assertThat(event.createdBy()).isEqualTo(AuditConstants.ANONYMOUS);
        assertThat(event.createdAt()).isNotNull();
        assertThat(event.metadata()).isNull();
    }

    @Test
    void builderHonoursExplicitValues() {
        UUID id = UUID.randomUUID();
        Instant ts = Instant.parse("2026-01-01T00:00:00Z");

        AuditEvent event = AuditEvent.builder()
                .id(id).type(TestData.ORDER_CREATED).metadata("{\"order\":1}")
                .createdBy(TestData.ALICE).createdAt(ts).build();

        assertThat(event).isEqualTo(new AuditEvent(id, TestData.ORDER_CREATED, "{\"order\":1}", TestData.ALICE, ts));
    }

    @Test
    void typeIsMandatory() {
        assertThatThrownBy(() -> AuditEvent.builder().build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void builtInTypesAreExposedAsConstants() {
        assertThat(AuditConstants.LOGIN).isEqualTo("LOGIN");
        assertThat(AuditConstants.LOGOUT).isEqualTo("LOGOUT");
    }
}
