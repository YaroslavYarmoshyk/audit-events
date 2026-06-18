package com.acme.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.acme.audit.constants.AuditConstants;
import org.jspecify.annotations.Nullable;

/**
 * Immutable audit event. The {@code metadata} field carries an optional payload
 * (free-form text or a JSON document produced from a {@code Map}).
 */
public record AuditEvent(
        UUID id,
        String type,
        @Nullable String metadata,
        String createdBy,
        Instant createdAt) {

    public AuditEvent {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(createdBy, "createdBy");
        Objects.requireNonNull(createdAt, "createdAt");
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Fluent builder with sensible defaults (random id, anonymous author, now). */
    public static final class Builder {
        private UUID id = UUID.randomUUID();
        private @Nullable String type;
        private @Nullable String metadata;
        private String createdBy = AuditConstants.ANONYMOUS;
        private Instant createdAt = Instant.now();

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder metadata(@Nullable String metadata) { this.metadata = metadata; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public AuditEvent build() {
            return new AuditEvent(id, Objects.requireNonNull(type, "type"), metadata, createdBy, createdAt);
        }
    }
}
