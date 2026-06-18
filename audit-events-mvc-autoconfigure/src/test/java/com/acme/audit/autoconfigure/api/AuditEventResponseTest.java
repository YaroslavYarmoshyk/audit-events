package com.acme.audit.autoconfigure.api;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import com.acme.audit.AuditEvent;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditEventResponseTest {

    private final JsonMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void createdAtSerializesAsSecondPrecisionDateTime() throws Exception {
        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .type(AuditConstants.LOGIN)
                .createdBy(TestData.ALICE)
                .createdAt(Instant.parse("2026-06-18T14:03:19.145931Z"))
                .build();

        String json = mapper.writeValueAsString(AuditEventResponse.from(event, ZoneId.of("UTC")));

        assertThat(json).contains("\"createdAt\":\"2026-06-18 14:03:19\"");
    }
}
