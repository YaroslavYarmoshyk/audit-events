package com.acme.audit.autoconfigure.webflux;

import java.util.Optional;

import org.jspecify.annotations.NonNull;
import reactor.core.publisher.Mono;

import org.springframework.core.Ordered;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

/**
 * Resolves the authenticated user from the reactive {@link ReactiveSecurityContextHolder} once per
 * request and writes it into the Reactor {@code Context} under {@link ReactiveAuditUserAccessor#KEY},
 * so downstream {@code audit.publish(...)} calls are attributed to that user.
 *
 * <p>Ordered at {@code 0} - after Spring Security's web filter chain (default order {@code -100}) -
 * so the security context is already established when this filter reads it. Anonymous and
 * unauthenticated requests are passed through untouched (no user written).
 */
public class ReactiveAuditUserWebFilter implements WebFilter, Ordered {

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // Resolve the user (if any) to an Optional, then invoke the chain exactly once. The chain is a
        // Mono<Void> (empty completion), so flatMap + switchIfEmpty would subscribe it twice - hence
        // defaultIfEmpty(Optional.empty()) to guarantee a single pass-through.
        return ReactiveSecurityContextHolder.getContext()
                .mapNotNull(SecurityContext::getAuthentication)
                .filter(ReactiveAuditUserWebFilter::isAuthenticatedUser)
                .map(Authentication::getName)
                .map(Optional::of)
                .defaultIfEmpty(Optional.empty())
                .flatMap(user -> user
                        .map(name -> chain.filter(exchange)
                                .contextWrite(ctx -> ctx.put(ReactiveAuditUserAccessor.KEY, name)))
                        .orElseGet(() -> chain.filter(exchange)));
    }

    private static boolean isAuthenticatedUser(Authentication authentication) {
        return authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getName() != null
                && !authentication.getName().isBlank();
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
