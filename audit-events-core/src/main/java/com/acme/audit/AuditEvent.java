package com.acme.audit;

import com.acme.audit.constants.AuditConstants;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit event. The {@code metadata} field carries an optional payload
 * (free-form text or a JSON document produced from a {@code Map}).
 *
 * <p>{@code createdAt} is a zoneless {@link LocalDateTime} holding the wall-clock time in the
 * library's configured zone.
 */
@Builder
public record AuditEvent(
        UUID id,
        String type,
        @Nullable String metadata,
        String createdBy,
        LocalDateTime createdAt) {

    public AuditEvent {
        id = (id != null) ? id : UUID.randomUUID();
        createdBy = (createdBy != null) ? createdBy : AuditConstants.ANONYMOUS;
        createdAt = (createdAt != null) ? createdAt : LocalDateTime.now();
        Objects.requireNonNull(type, "type");
    }
}
