package com.acme.audit.autoconfigure.testsupport;

import java.util.ArrayList;
import java.util.List;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Shared security/publisher fixtures for the auto-configuration test suites: principal tokens and
 * the {@link RecordingPublisher}, so the servlet and reactive modules assert attribution against a
 * single definition instead of re-declaring them per test.
 *
 * <p>Lives in {@code audit-events-mvc-autoconfigure} and is shared with the webflux module through
 * that module's {@code test-jar}. Deliberately depends only on core + spring-security-core (both on
 * every consumer's test classpath); JDBC fixtures live in {@link TestDataSources} to keep this class
 * free of spring-jdbc, and the shared string constants live in {@link TestData} (core).
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestDataFactory {

    /** An unauthenticated token carrying {@code user} as the principal and the canonical credentials. */
    public static TestingAuthenticationToken principal(String user) {
        return new TestingAuthenticationToken(user, TestData.CREDENTIALS);
    }

    /** As {@link #principal(String)} but flagged authenticated, for filters that read a live security context. */
    public static TestingAuthenticationToken authenticatedPrincipal(String user) {
        TestingAuthenticationToken token = principal(user);
        token.setAuthenticated(true);
        return token;
    }

    /** The framework's anonymous authentication, which must fall back to {@link AuditConstants#ANONYMOUS}. */
    public static Authentication anonymous() {
        return new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
    }

    /** Captures every published event so assertions can inspect the resolved attribution. */
    public static final class RecordingPublisher implements AuditEventPublisher {
        public final List<Captured> captured = new ArrayList<>();

        @Override
        public void publish(String type, @Nullable Object metadata) {
            captured.add(new Captured(AuditConstants.ANONYMOUS, type, metadata));
        }

        @Override
        public void publishAs(String createdBy, String type, @Nullable Object metadata) {
            captured.add(new Captured(createdBy, type, metadata));
        }
    }

    public record Captured(String createdBy, String type, @Nullable Object metadata) {
    }
}
