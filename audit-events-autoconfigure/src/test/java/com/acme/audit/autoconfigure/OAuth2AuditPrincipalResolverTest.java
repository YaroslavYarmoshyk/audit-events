package com.acme.audit.autoconfigure;

import java.util.List;
import java.util.Map;

import com.acme.audit.AuditPrincipalResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Resolution rules of the OAuth2-only default: the {@code preferred_username} claim wins, falling
 * back to {@link org.springframework.security.core.Authentication#getName()} when it is absent, and
 * any non-OAuth2 principal is rejected so the application is forced to register its own resolver.
 */
@DisplayName("OAuth2 audit principal resolver")
class OAuth2AuditPrincipalResolverTest {

    private final AuditPrincipalResolver resolver = new OAuth2AuditPrincipalResolver();

    /** Carries the opaque {@code sub} as the name attribute plus a friendlier {@code preferred_username}. */
    private final TestingAuthenticationToken oauthUser = new TestingAuthenticationToken(
            new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_USER")),
                    Map.of("sub", "opaque-id-123", "preferred_username", "alice"),
                    "sub"),
            "credentials");

    @Test
    @DisplayName("Prefers the preferred_username claim")
    void prefersPreferredUsernameClaim() {
        assertThat(resolver.resolve(oauthUser)).isEqualTo("alice");
    }

    @Test
    @DisplayName("Falls back to the authentication name when the claim is absent")
    void fallsBackToNameWhenClaimAbsent() {
        TestingAuthenticationToken noPreferredUsername = new TestingAuthenticationToken(
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("ROLE_USER")),
                        Map.of("sub", "opaque-id-123"),
                        "sub"),
                "credentials");

        assertThat(resolver.resolve(noPreferredUsername)).isEqualTo(noPreferredUsername.getName());
    }

    @Test
    @DisplayName("Rejects a non-OAuth2 principal so the app registers its own resolver")
    void rejectsNonOAuth2Principal() {
        TestingAuthenticationToken plain = new TestingAuthenticationToken("bob", "credentials");

        assertThatThrownBy(() -> resolver.resolve(plain))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AuditPrincipalResolver");
    }
}
