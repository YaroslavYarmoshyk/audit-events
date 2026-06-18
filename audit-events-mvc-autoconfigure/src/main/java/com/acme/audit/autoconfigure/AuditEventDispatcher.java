package com.acme.audit.autoconfigure;

import com.acme.audit.AuditEvent;
import com.acme.audit.autoconfigure.constants.AuditAutoConfigurationConstants;
import com.acme.audit.spi.AuditEventStore;
import com.acme.audit.spi.AuditRecordedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists audit events off the business thread. Listens for {@link AuditRecordedEvent}
 * after the surrounding transaction commits (so rolled-back work is not audited), with
 * {@code fallbackExecution = true} so events published outside any transaction are still
 * handled. Persistence is then dispatched to a virtual-thread executor.
 *
 * <p>Failures are swallowed (logged at DEBUG + counted) - auditing must never break the
 * caller.
 */
@Slf4j
@RequiredArgsConstructor
public class AuditEventDispatcher {
    private final AuditEventStore store;
    private final AsyncTaskExecutor executor;
    private final boolean async;
    private final ObjectProvider<MeterRegistry> meterRegistry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(AuditRecordedEvent event) {
        if (async) {
            executor.execute(() -> persist(event.event()));
        } else {
            persist(event.event());
        }
    }

    private void persist(AuditEvent event) {
        try {
            store.save(event);
            count(AuditAutoConfigurationConstants.METRIC_EVENTS_PERSISTED);
            log.trace("Audit event persisted id: {} type: {}", event.id(), event.type());
        } catch (RuntimeException ex) {
            count(AuditAutoConfigurationConstants.METRIC_EVENTS_FAILED);
            log.warn("Audit event persistence failed id: {} type: {}: {}", event.id(), event.type(), ex.toString());
        }
    }

    private void count(String metric) {
        MeterRegistry registry = meterRegistry.getIfAvailable();
        if (registry != null) {
            registry.counter(metric).increment();
        }
    }
}
