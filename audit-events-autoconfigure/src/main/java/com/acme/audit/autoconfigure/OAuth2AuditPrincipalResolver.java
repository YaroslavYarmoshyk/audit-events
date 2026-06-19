package com.acme.audit.autoconfigure;

import com.acme.audit.AuditPrincipalResolver;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.util.ClassUtils;

/**
 * {@link AuditPrincipalResolver} for interactive OAuth2/OIDC logins: it records the
 * {@code preferred_username} claim of the {@link OAuth2AuthenticatedPrincipal} rather than
 * {@link Authentication#getName()}, which on such a principal is the configured name attribute -
 * frequently the opaque {@code sub} claim, a poor audit trail. When the claim is absent it falls back
 * to {@code getName()}.
 *
 * <p>It deliberately handles <strong>only</strong> OAuth2/OIDC principals. Any other authentication
 * (JWT/bearer resource-server tokens, username/password, custom principals) - and the case where
 * {@code spring-security-oauth2-core} is not on the classpath at all - throws
 * {@link IllegalStateException}. This is intentional: there is no sensible universal default for
 * those, so applications must register their own {@link AuditPrincipalResolver} bean (which replaces
 * this one) to attribute them.
 */
public class OAuth2AuditPrincipalResolver implements AuditPrincipalResolver {

    /** Standard OIDC claim carrying the human-readable username. */
    static final String PREFERRED_USERNAME = "preferred_username";

    private static final boolean OAUTH2_PRESENT = ClassUtils.isPresent(
            "org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal",
            OAuth2AuditPrincipalResolver.class.getClassLoader());

    @Override
    public String resolve(Authentication authentication) {
        if (!OAUTH2_PRESENT || !OAuth2Support.isOAuth2Principal(authentication)) {
            throw new IllegalStateException(
                    "OAuth2AuditPrincipalResolver resolves only OAuth2/OIDC logins, but the authentication "
                            + "principal was " + describePrincipal(authentication) + ". Register a custom "
                            + "AuditPrincipalResolver bean to attribute this authentication.");
        }
        return OAuth2Support.preferredUsernameOrName(authentication);
    }

    private static String describePrincipal(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        return principal != null ? principal.getClass().getName() : "null";
    }

    /** Touches {@link OAuth2AuthenticatedPrincipal}; loaded only when oauth2-core is on the classpath. */
    private static final class OAuth2Support {

        static boolean isOAuth2Principal(Authentication authentication) {
            return authentication.getPrincipal() instanceof OAuth2AuthenticatedPrincipal;
        }

        static String preferredUsernameOrName(Authentication authentication) {
            OAuth2AuthenticatedPrincipal principal = (OAuth2AuthenticatedPrincipal) authentication.getPrincipal();
            Object preferredUsername = principal.getAttributes().get(PREFERRED_USERNAME);
            return preferredUsername != null && !preferredUsername.toString().isBlank()
                    ? preferredUsername.toString()
                    : authentication.getName();
        }
    }
}
