package com.acme.audit.autoconfigure.api;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import com.acme.audit.AuditEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.jspecify.annotations.Nullable;

/**
 * Read-side representation of an audit event. The stored UTC {@code createdAt} instant is rendered
 * as a {@link LocalDateTime} in the configured zone.
 */
public record AuditEventResponse(
        UUID id,
        String type,
        @Nullable String metadata,
        String createdBy,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    public static AuditEventResponse from(AuditEvent event, ZoneId zone) {
        return new AuditEventResponse(event.id(), event.type(), event.metadata(), event.createdBy(),
                LocalDateTime.ofInstant(event.createdAt(), zone));
    }
}
