package com.acme.audit.autoconfigure.api;

import java.time.LocalDateTime;
import java.util.UUID;

import com.acme.audit.AuditEvent;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.jspecify.annotations.Nullable;

/**
 * Read-side representation of an audit event. {@code createdAt} is the stored wall-clock
 * {@link LocalDateTime} in the library's configured zone.
 */
public record AuditEventResponse(
        UUID id,
        String type,
        @Nullable String metadata,
        String createdBy,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {

    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.id(), event.type(), event.metadata(), event.createdBy(),
                event.createdAt());
    }
}
