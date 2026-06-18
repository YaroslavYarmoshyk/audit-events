package com.acme.audit.autoconfigure.webflux;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.acme.audit.autoconfigure.testsupport.TestDataFactory;
import com.acme.audit.testsupport.TestData;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the filter's deterministic contract: it lifts the authenticated user into the Reactor
 * {@code Context} under {@link ReactiveAuditUserAccessor#KEY} and invokes the downstream chain
 * exactly once. The final hop - Reactor restoring that context value into the thread-local read by
 * the {@code AuditorAware} - is handled by Micrometer/Reactor automatic context propagation and is
 * exercised end-to-end in a running application rather than asserted here.
 */
class ReactiveAuditUserAttributionTest {

    private final WebFilter filter = new ReactiveAuditUserWebFilter();
    private final MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/x"));

    @Test
    void authenticatedUserIsWrittenToContextAndChainRunsOnce() {
        RecordingChain chain = new RecordingChain();

        StepVerifier.create(filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(
                                TestDataFactory.authenticatedPrincipal(TestData.ALICE))))
                .verifyComplete();

        assertThat(chain.userSeen.get()).isEqualTo(TestData.ALICE);
        assertThat(chain.invocations.get()).isEqualTo(1);
    }

    @Test
    void anonymousRequestWritesNoUserAndChainRunsOnce() {
        RecordingChain chain = new RecordingChain();

        StepVerifier.create(filter.filter(exchange, chain)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(TestDataFactory.anonymous())))
                .verifyComplete();

        assertThat(chain.userSeen.get()).isEqualTo(ABSENT);
        assertThat(chain.invocations.get()).isEqualTo(1);
    }

    @Test
    void unauthenticatedRequestWritesNoUserAndChainRunsOnce() {
        RecordingChain chain = new RecordingChain();

        // No security context written at all.
        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.userSeen.get()).isEqualTo(ABSENT);
        assertThat(chain.invocations.get()).isEqualTo(1);
    }

    private static final String ABSENT = "ABSENT";

    /** Records how many times it ran and the user present in the Reactor context at that point. */
    private static final class RecordingChain implements WebFilterChain {
        private final AtomicInteger invocations = new AtomicInteger();
        private final AtomicReference<String> userSeen = new AtomicReference<>(ABSENT);

        @NonNull
        @Override
        public Mono<Void> filter(@NonNull ServerWebExchange exchange) {
            return Mono.deferContextual(view -> {
                invocations.incrementAndGet();
                userSeen.set(view.getOrDefault(ReactiveAuditUserAccessor.KEY, ABSENT));
                return Mono.empty();
            });
        }
    }
}
