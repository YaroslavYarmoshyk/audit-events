package com.acme.audit.testsupport;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Canonical test identifiers shared across every audit-events test suite. Published from
 * audit-events-core as a {@code test-jar} so the downstream modules assert against one set of values.
 *
 * <p>Built-in event types ({@code LOGIN}, {@code LOGOUT}) and the anonymous auditor already live in
 * {@link com.acme.audit.constants.AuditConstants} - reference those directly instead of redefining
 * them here. This class holds only test-domain values that have no production constant.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestData {

    /** Canonical authenticated principals. */
    public static final String ALICE = "alice";
    public static final String BOB = "bob";

    /** Credentials paired with a principal when building authentication tokens. */
    public static final String CREDENTIALS = "creds";

    /** Sample domain event types used to exercise type filtering / metadata capture. */
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_CANCELLED = "ORDER_CANCELLED";
}
