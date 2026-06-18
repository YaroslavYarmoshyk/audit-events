package com.acme.audit.spi;

import com.acme.audit.AuditEvent;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

/**
 * Storage SPI. Two implementations ship with the library (in-memory and JDBC);
 * clients may provide their own and it will be picked up via
 * {@code @ConditionalOnMissingBean}.
 */
public interface AuditEventStore {

    /** Persist a single event. Called from the async dispatcher. */
    void save(AuditEvent event);

    /** Paginated query for the read side. */
    Page<AuditEvent> search(AuditSearchCriteria criteria, Pageable pageable);

    /**
     * Delete up to {@code batchSize} events older than {@code cutoff}.
     *
     * @return the number of rows deleted (0 when nothing matched)
     */
    int deleteOlderThan(Instant cutoff, int batchSize);
}
