package com.acme.audit;

import java.time.LocalDateTime;
import java.util.UUID;

import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuditEvent")
class AuditEventTest {

    @Test
    @DisplayName("Builder applies sensible defaults for unset fields")
    void builderAppliesSensibleDefaults() {
        AuditEvent event = AuditEvent.builder().type(AuditConstants.LOGIN).build();

        assertThat(event.id()).isNotNull();
        assertThat(event.type()).isEqualTo(AuditConstants.LOGIN);
        assertThat(event.createdBy()).isEqualTo(AuditConstants.ANONYMOUS);
        assertThat(event.createdAt()).isNotNull();
        assertThat(event.metadata()).isNull();
    }

    @Test
    @DisplayName("Builder honours explicitly supplied values")
    void builderHonoursExplicitValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime ts = LocalDateTime.parse("2026-01-01T00:00:00");

        AuditEvent event = AuditEvent.builder()
                .id(id).type(TestData.ORDER_CREATED).metadata("{\"order\":1}")
                .createdBy(TestData.ALICE).createdAt(ts).build();

        assertThat(event).isEqualTo(new AuditEvent(id, TestData.ORDER_CREATED, "{\"order\":1}", TestData.ALICE, ts));
    }

    @Test
    @DisplayName("Type is mandatory and a missing one is rejected")
    void typeIsMandatory() {
        assertThatThrownBy(() -> AuditEvent.builder().build())
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Built-in event types are exposed as constants")
    void builtInTypesAreExposedAsConstants() {
        assertThat(AuditConstants.LOGIN).isEqualTo("LOGIN");
        assertThat(AuditConstants.LOGOUT).isEqualTo("LOGOUT");
    }
}
