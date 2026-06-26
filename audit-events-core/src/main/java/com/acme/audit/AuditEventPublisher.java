package com.acme.audit;

import org.jspecify.annotations.Nullable;

/**
 * Primary entry point for recording audit events. {@code type} is mandatory,
 * {@code metadata} is optional. Implementations are non-blocking from the
 * caller's perspective (persistence happens asynchronously).
 */
public interface AuditEventPublisher {

    /**
     * Records an event with optional metadata. A {@code String} (or any
     * {@link CharSequence}) is stored as-is; any other object is serialized to a
     * JSON {@code metadata} string.
     */
    void publish(String type, @Nullable Object metadata);

    /**
     * Records an event with no metadata.
     */
    default void publish(String type) {
        publish(type, null);
    }

    /**
     * Records an event whose type is an enum constant, stored as its {@link Enum#name()}.
     */
    default void publish(Enum<?> type) {
        publish(type.name());
    }

    /**
     * As {@link #publish(String, Object)}, with the type given as an enum constant.
     */
    default void publish(Enum<?> type, @Nullable Object metadata) {
        publish(type.name(), metadata);
    }

    /**
     * Records an event attributed to an explicit {@code createdBy}, bypassing the ambient
     * auditor resolution. Intended for call sites that know the actor but cannot rely on the
     * current thread's context - notably security LOGIN/LOGOUT events, which fire before the
     * {@code SecurityContext} is established (or after it is cleared).
     *
     * <p>The default implementation ignores {@code createdBy} and delegates to
     * {@link #publish(String, Object)}; implementations that can honor an explicit actor
     * should override it.
     */
    default void publishAs(String createdBy, String type, @Nullable Object metadata) {
        publish(type, metadata);
    }

    /**
     * As {@link #publishAs(String, String, Object)}, with the type given as an enum constant.
     */
    default void publishAs(String createdBy, Enum<?> type, @Nullable Object metadata) {
        publishAs(createdBy, type.name(), metadata);
    }
}
