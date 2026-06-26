package com.acme.audit.autoconfigure.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.acme.audit.AuditEvent;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuditEventResponse serialization")
class AuditEventResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    @DisplayName("The createdAt field serializes as a second-precision date-time")
    void createdAtSerializesAsSecondPrecisionDateTime() throws Exception {
        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .type(AuditConstants.LOGIN)
                .createdBy(TestData.ALICE)
                .createdAt(LocalDateTime.parse("2026-06-18T14:03:19.145931"))
                .build();

        String json = mapper.writeValueAsString(AuditEventResponse.from(event));

        assertThat(json).contains("\"createdAt\":\"2026-06-18 14:03:19\"");
    }
}
