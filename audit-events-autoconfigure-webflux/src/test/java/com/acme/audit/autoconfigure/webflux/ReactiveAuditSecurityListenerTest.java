package com.acme.audit.autoconfigure.webflux;

import com.acme.audit.autoconfigure.testsupport.TestDataFactory;
import com.acme.audit.autoconfigure.testsupport.TestDataFactory.RecordingPublisher;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class ReactiveAuditSecurityListenerTest {
    private final RecordingPublisher publisher = new RecordingPublisher();

    @Test
    void authenticationSuccessRecordsLoginAttributedToEventPrincipal() {
        listener(true, true).onAuthenticationSuccess(
                new AuthenticationSuccessEvent(TestDataFactory.principal(TestData.ALICE)));

        assertThat(publisher.captured).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(AuditConstants.LOGIN);
            assertThat(event.createdBy()).isEqualTo(TestData.ALICE);
            assertThat(event.metadata()).isNull();
        });
    }

    @Test
    void logoutRecordsLogoutAttributedToEventPrincipal() {
        listener(true, true).onLogoutSuccess(new LogoutSuccessEvent(TestDataFactory.principal(TestData.BOB)));

        assertThat(publisher.captured).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo(AuditConstants.LOGOUT);
            assertThat(event.createdBy()).isEqualTo(TestData.BOB);
        });
    }

    @Test
    void disabledFlagsSuppressEvents() {
        ReactiveAuditSecurityListener listener = listener(false, false);

        listener.onAuthenticationSuccess(new AuthenticationSuccessEvent(TestDataFactory.principal(TestData.ALICE)));
        listener.onLogoutSuccess(new LogoutSuccessEvent(TestDataFactory.principal(TestData.BOB)));

        assertThat(publisher.captured).isEmpty();
    }

    @Test
    void anonymousPrincipalFallsBackToAnonymous() {
        listener(true, true).onAuthenticationSuccess(new AuthenticationSuccessEvent(TestDataFactory.anonymous()));

        assertThat(publisher.captured).singleElement()
                .satisfies(event -> assertThat(event.createdBy()).isEqualTo(AuditConstants.ANONYMOUS));
    }

    private ReactiveAuditSecurityListener listener(boolean login, boolean logout) {
        return new ReactiveAuditSecurityListener(publisher, login, logout, Authentication::getName);
    }
}
