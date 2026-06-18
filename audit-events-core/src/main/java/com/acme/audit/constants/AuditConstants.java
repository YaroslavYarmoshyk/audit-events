package com.acme.audit.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Shared, cross-module constant values for the audit library.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditConstants {
    /** Auditor used when no authenticated principal can be resolved. */
    public static final String ANONYMOUS = "anonymous";

    /** Built-in event type recorded on a successful authentication. */
    public static final String LOGIN = "LOGIN";

    /** Built-in event type recorded on a successful logout. */
    public static final String LOGOUT = "LOGOUT";
}
