package com.acme.audit;

import org.jspecify.annotations.Nullable;

/**
 * Publisher that discards every event. Installed by the {@link Audit} facade until a real
 * publisher is wired, and used when auditing is disabled
 * ({@code framework.audit-events.enabled=false}), so application code can always call the
 * {@link AuditEventPublisher} API safely.
 */
public final class NoOpAuditEventPublisher implements AuditEventPublisher {

    @Override
    public void publish(String type, @Nullable Object metadata) {
        // intentionally does nothing
    }
}
