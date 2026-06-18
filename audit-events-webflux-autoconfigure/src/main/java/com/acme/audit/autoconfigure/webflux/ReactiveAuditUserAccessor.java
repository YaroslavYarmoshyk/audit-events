package com.acme.audit.autoconfigure.webflux;

import io.micrometer.context.ThreadLocalAccessor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Bridges the reactive "current user" into a thread-local, so the synchronous
 * {@code AuditorAware<String>} the publisher relies on can read it on the reactive stack.
 *
 * <p>{@link ReactiveAuditUserWebFilter} writes the resolved username into the Reactor {@code Context}
 * under {@link #KEY}. With Reactor automatic context propagation enabled, this accessor restores that
 * value to {@link #current()} around each operator, so a synchronous {@code audit.publish(...)} run
 * inside the reactive chain is attributed to the right actor.
 */
public final class ReactiveAuditUserAccessor implements ThreadLocalAccessor<String> {

    /** Reactor {@code Context} / propagation key under which the resolved username is stored. */
    public static final String KEY = "com.acme.audit.user";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    /** The username restored for the current thread, or {@code null} when none is in scope. */
    public static @Nullable String current() {
        return CURRENT.get();
    }

    @NonNull
    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public @Nullable String getValue() {
        return CURRENT.get();
    }

    @Override
    public void setValue(@NonNull String value) {
        CURRENT.set(value);
    }

    @Override
    public void setValue() {
        CURRENT.remove();
    }
}
