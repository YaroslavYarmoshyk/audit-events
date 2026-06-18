package com.acme.audit.autoconfigure;

import java.time.Clock;
import java.time.Instant;

import com.acme.audit.spi.AuditEventStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Deletes audit events older than {@code framework.audit-events.retention.max-age} on a cron schedule,
 * in bounded batches to avoid long locks. Off by default.
 *
 * <p>In a multi-instance deployment guard this with leader election (e.g. ShedLock) to
 * avoid concurrent purges.
 */
@Slf4j
@RequiredArgsConstructor
public class AuditRetentionJob {
    private final AuditEventStore store;
    private final AuditProperties.Retention props;
    private final Clock clock;

    @Scheduled(cron = "${framework.audit-events.retention.cron:0 0 3 * * *}")
    public void purge() {
        Instant cutoff = Instant.now(clock).atZone(clock.getZone()).minus(props.getMaxAge()).toInstant();
        int totalDeleted = 0;
        int deleted;
        do {
            deleted = store.deleteOlderThan(cutoff, props.getBatchSize());
            totalDeleted += deleted;
        } while (deleted >= props.getBatchSize());

        if (totalDeleted > 0) {
            log.debug("Audit retention purged {} events older than {}", totalDeleted, cutoff);
        }
    }
}
