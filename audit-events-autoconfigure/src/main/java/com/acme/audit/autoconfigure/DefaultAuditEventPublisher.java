package com.acme.audit.autoconfigure;

import java.time.Clock;

import com.acme.audit.AuditEvent;
import com.acme.audit.AuditEventPublisher;
import com.acme.audit.constants.AuditConstants;
import com.acme.audit.spi.AuditRecordedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.AuditorAware;

/**
 * Default publisher: resolves the auditor and timestamp, then hands the event to
 * Spring's {@link ApplicationEventPublisher}. Persistence happens elsewhere
 * (asynchronously), so this method never blocks on I/O.
 */
@Slf4j
@RequiredArgsConstructor
public class DefaultAuditEventPublisher implements AuditEventPublisher {
    private final ApplicationEventPublisher events;
    private final AuditorAware<String> auditorAware;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String type, @Nullable Object metadata) {
        publishInternal(type, metadata, auditorAware.getCurrentAuditor().orElse(AuditConstants.ANONYMOUS));
    }

    @Override
    public void publishAs(String createdBy, String type, @Nullable Object metadata) {
        publishInternal(type, metadata, createdBy);
    }

    private void publishInternal(String type, @Nullable Object metadata, String createdBy) {
        AuditEvent event = AuditEvent.builder()
                .type(type)
                .metadata(toMetadataString(metadata))
                .createdBy(createdBy)
                .createdAt(clock.instant())
                .build();
        log.debug("Audit event received type: {} createdBy: {}", event.type(), event.createdBy());
        events.publishEvent(new AuditRecordedEvent(event));
    }

    /**
     * Converts arbitrary metadata to its stored form: a {@link CharSequence} is kept as-is,
     * anything else is serialized to JSON (falling back to {@code toString()} on failure).
     */
    private @Nullable String toMetadataString(@Nullable Object metadata) {
        if (metadata == null) {
            return null;
        }
        if (metadata instanceof CharSequence text) {
            return text.toString();
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException ex) {
            log.debug("Failed to serialize audit metadata, falling back to toString(): {}", ex.toString());
            return metadata.toString();
        }
    }
}
