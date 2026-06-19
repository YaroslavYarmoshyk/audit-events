package com.acme.audit.autoconfigure.security;

import java.util.concurrent.atomic.AtomicInteger;

import com.acme.audit.AuditPrincipalResolver;
import com.acme.audit.autoconfigure.testsupport.TestDataFactory;
import com.acme.audit.autoconfigure.testsupport.TestDataFactory.RecordingPublisher;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.ServerLogoutSuccessHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the reactive LOGIN/LOGOUT handler decorators: they record the event with the
 * principal from the callback, fall back to anonymous, and always run the wrapped delegate.
 */
class AuditSecurityHandlersTest {

    private final RecordingPublisher publisher = new RecordingPublisher();
    private final AuditPrincipalResolver resolver = Authentication::getName;
    private final WebFilterExchange webFilterExchange = new WebFilterExchange(
            MockServerWebExchange.from(MockServerHttpRequest.get("/x")), exchange -> Mono.empty());

    @Test
    void authenticationSuccessRecordsLoginAndDelegates() {
        CountingSuccessHandler delegate = new CountingSuccessHandler();
        ServerAuthenticationSuccessHandler handler =
                new AuditServerAuthenticationSuccessHandler(delegate, publisher, resolver);

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, TestDataFactory.principal(TestData.ALICE)))
                .verifyComplete();

        assertThat(publisher.captured).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(AuditConstants.LOGIN);
            assertThat(event.createdBy()).isEqualTo(TestData.ALICE);
            assertThat(event.metadata()).isNull();
        });
        assertThat(delegate.count).hasValue(1);
    }

    @Test
    void logoutRecordsLogoutAndDelegates() {
        CountingLogoutHandler delegate = new CountingLogoutHandler();
        ServerLogoutSuccessHandler handler =
                new AuditServerLogoutSuccessHandler(delegate, publisher, resolver);

        StepVerifier.create(handler.onLogoutSuccess(webFilterExchange, TestDataFactory.principal(TestData.BOB)))
                .verifyComplete();

        assertThat(publisher.captured).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(AuditConstants.LOGOUT);
            assertThat(event.createdBy()).isEqualTo(TestData.BOB);
        });
        assertThat(delegate.count).hasValue(1);
    }

    @Test
    void anonymousPrincipalFallsBackToAnonymous() {
        ServerAuthenticationSuccessHandler handler =
                new AuditServerAuthenticationSuccessHandler(new CountingSuccessHandler(), publisher, resolver);

        StepVerifier.create(handler.onAuthenticationSuccess(webFilterExchange, TestDataFactory.anonymous()))
                .verifyComplete();

        assertThat(publisher.captured).singleElement()
                .satisfies(event -> assertThat(event.createdBy()).isEqualTo(AuditConstants.ANONYMOUS));
    }

    @Test
    void logoutWithoutAuthenticationRecordsNothingButDelegates() {
        CountingLogoutHandler delegate = new CountingLogoutHandler();
        ServerLogoutSuccessHandler handler =
                new AuditServerLogoutSuccessHandler(delegate, publisher, resolver);

        StepVerifier.create(handler.onLogoutSuccess(webFilterExchange, null)).verifyComplete();

        assertThat(publisher.captured).isEmpty();
        assertThat(delegate.count).hasValue(1);
    }

    /** Records that the delegated success handler ran; returns empty completion like the real ones. */
    static final class CountingSuccessHandler implements ServerAuthenticationSuccessHandler {
        final AtomicInteger count = new AtomicInteger();

        @NonNull
        @Override
        public Mono<Void> onAuthenticationSuccess(@NonNull WebFilterExchange exchange, @NonNull Authentication authentication) {
            count.incrementAndGet();
            return Mono.empty();
        }
    }

    /** Records that the delegated logout handler ran; returns empty completion like the real ones. */
    static final class CountingLogoutHandler implements ServerLogoutSuccessHandler {
        final AtomicInteger count = new AtomicInteger();

        @NonNull
        @Override
        public Mono<Void> onLogoutSuccess(@NonNull WebFilterExchange exchange, @Nullable Authentication authentication) {
            count.incrementAndGet();
            return Mono.empty();
        }
    }
}
