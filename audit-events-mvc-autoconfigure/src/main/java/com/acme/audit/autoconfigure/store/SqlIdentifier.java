package com.acme.audit.autoconfigure.store;

import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Validates a configurable SQL identifier (the audit table name). The value is
 * interpolated into SQL, so it must never come from untrusted input; this guard
 * keeps it to a safe, DB-agnostic character set.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SqlIdentifier {
    private static final Pattern VALID = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

    public static String validate(String identifier) {
        if (identifier == null || !VALID.matcher(identifier).matches()) {
            throw new IllegalArgumentException(
                    "Illegal audit table name '" + identifier + "'. Allowed: letters, digits, underscore; "
                            + "must not start with a digit.");
        }
        return identifier;
    }
}
