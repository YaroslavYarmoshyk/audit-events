package com.acme.audit.spi;

import java.time.LocalDateTime;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * Query criteria for the read side. Every filter is optional and multi-valued,
 * so a single criteria object covers every combination the REST API exposes
 * (by type/types, creator/creators, with/without time window).
 *
 * <p>A {@code null} or empty filter means "no constraint on that dimension".
 */
public record AuditSearchCriteria(
        @Nullable Set<String> types,
        @Nullable Set<String> creators,
        @Nullable LocalDateTime from,
        @Nullable LocalDateTime to) {

    public static AuditSearchCriteria all() {
        return new AuditSearchCriteria(null, null, null, null);
    }
}
