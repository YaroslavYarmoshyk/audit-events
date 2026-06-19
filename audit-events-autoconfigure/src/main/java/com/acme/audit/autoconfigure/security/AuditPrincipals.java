package com.acme.audit.autoconfigure.security;

import com.acme.audit.AuditPrincipalResolver;
import com.acme.audit.constants.AuditConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

/**
 * Resolves the auditor name carried by a Spring Security {@link Authentication}, falling back to
 * {@link AuditConstants#ANONYMOUS} for anonymous tokens or when the resolver yields a blank value.
 * Shared by the servlet LOGIN/LOGOUT listener and the reactive handler decorators so attribution is
 * defined once for both stacks.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class AuditPrincipals {

    static String resolve(Authentication authentication, AuditPrincipalResolver resolver) {
        String name = !(authentication instanceof AnonymousAuthenticationToken)
                ? resolver.resolve(authentication)
                : null;
        return (name != null && !name.isBlank()) ? name : AuditConstants.ANONYMOUS;
    }
}
