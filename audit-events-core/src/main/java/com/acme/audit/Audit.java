package com.acme.audit;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Static facade for ergonomic, static-import based usage:
 *
 * <pre>{@code
 * import static com.acme.audit.Audit.record;
 *
 * record("ORDER_CREATED", "Order #123");
 * }</pre>
 *
 * <p>The delegate is injected once by auto-configuration. When auditing is disabled
 * a no-op publisher is installed, so calls are always safe.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Audit {
    private static volatile AuditEventPublisher delegate = new NoOpAuditEventPublisher();

    /** Wired by auto-configuration. Not intended for application code. */
    public static void setPublisher(AuditEventPublisher publisher) {
        delegate = (publisher != null) ? publisher : new NoOpAuditEventPublisher();
    }

    public static void record(String type) {
        delegate.publish(type);
    }

    public static void record(String type, @Nullable Object metadata) {
        delegate.publish(type, metadata);
    }

    /** Records an event whose type is an enum constant, stored as its {@link Enum#name()}. */
    public static void record(Enum<?> type) {
        delegate.publish(type);
    }

    public static void record(Enum<?> type, @Nullable Object metadata) {
        delegate.publish(type, metadata);
    }
}
