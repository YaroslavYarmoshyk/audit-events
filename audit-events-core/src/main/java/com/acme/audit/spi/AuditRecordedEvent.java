package com.acme.audit.spi;

import com.acme.audit.AuditEvent;

/**
 * Internal application event carrying a fully-resolved {@link AuditEvent}.
 * Published by the {@code AuditEventPublisher} and consumed asynchronously by the
 * dispatcher. A plain record is used (Spring supports arbitrary event payloads).
 */
public record AuditRecordedEvent(AuditEvent event) {
}
