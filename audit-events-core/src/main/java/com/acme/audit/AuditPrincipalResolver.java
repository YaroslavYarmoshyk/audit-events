package com.acme.audit;

import org.springframework.security.core.Authentication;

/**
 * Resolves the auditor ({@code createdBy}) recorded on an audit event from the current
 * {@link Authentication}. This is the single point that decides <em>who</em> an event is attributed
 * to - used for LOGIN/LOGOUT and for the {@code createdBy} of every audited event.
 *
 * <p>The auto-configuration registers a default geared to interactive OAuth2/OIDC logins: it records
 * the {@code preferred_username} claim and <strong>throws</strong> for any other authentication
 * (JWT/bearer, username/password, custom principals). Such applications must declare their own bean -
 * any {@code Authentication}-to-name mapping you need (a custom claim, a composite "name &lt;email&gt;",
 * a lookup, ...) - which replaces the default:
 *
 * <pre>{@code
 * @Bean
 * AuditPrincipalResolver auditPrincipalResolver() {
 *     return authentication -> {
 *         if (authentication.getPrincipal() instanceof MyUser user) {
 *             return user.getEmployeeId();
 *         }
 *         return authentication.getName();
 *     };
 * }
 * }</pre>
 *
 * <p>Implementations are only invoked for non-anonymous authentications. Returning {@code null} or a
 * blank value attributes the event to the anonymous fallback.
 */
@FunctionalInterface
public interface AuditPrincipalResolver {

    /**
     * Resolves the auditor name for {@code authentication}, or {@code null}/blank to fall back to the
     * anonymous auditor.
     */
    String resolve(Authentication authentication);
}
