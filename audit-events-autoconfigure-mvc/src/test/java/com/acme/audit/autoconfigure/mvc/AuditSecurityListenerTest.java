package com.acme.audit.autoconfigure.mvc;

import com.acme.audit.autoconfigure.testsupport.TestDataFactory;
import com.acme.audit.autoconfigure.testsupport.TestDataFactory.RecordingPublisher;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.testsupport.TestData;
import org.junit.jupiter.api.Test;

import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSecurityListenerTest {
    private final RecordingPublisher publisher = new RecordingPublisher();

    @Test
    void interactiveLoginRecordsLoginAttributedToEventPrincipal() {
        listener(true, true).onInteractiveAuthenticationSuccess(interactiveLogin());

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
        AuditSecurityListener listener = listener(false, false);

        listener.onInteractiveAuthenticationSuccess(interactiveLogin());
        listener.onLogoutSuccess(new LogoutSuccessEvent(TestDataFactory.principal(TestData.BOB)));

        assertThat(publisher.captured).isEmpty();
    }

    @Test
    void anonymousPrincipalFallsBackToAnonymous() {
        listener(true, true).onInteractiveAuthenticationSuccess(
                new InteractiveAuthenticationSuccessEvent(TestDataFactory.anonymous(), getClass()));

        assertThat(publisher.captured).singleElement()
                .satisfies(event -> assertThat(event.createdBy()).isEqualTo(AuditConstants.ANONYMOUS));
    }

    private AuditSecurityListener listener(boolean login, boolean logout) {
        return new AuditSecurityListener(publisher, login, logout, Authentication::getName);
    }

    private InteractiveAuthenticationSuccessEvent interactiveLogin() {
        return new InteractiveAuthenticationSuccessEvent(TestDataFactory.principal(TestData.ALICE), getClass());
    }
}
