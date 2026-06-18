package com.acme.audit.autoconfigure.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Internal constant values used by the audit auto-configuration (thread naming, metric names).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditAutoConfigurationConstants {
    /** Thread-name prefix for the audit persistence executor. */
    public static final String THREAD_NAME_PREFIX = "audit-events-";

    /** Micrometer counter incremented when an event is persisted successfully. */
    public static final String METRIC_EVENTS_PERSISTED = "audit.events.persisted";

    /** Micrometer counter incremented when event persistence fails. */
    public static final String METRIC_EVENTS_FAILED = "audit.events.failed";
}
