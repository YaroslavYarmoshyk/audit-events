package com.acme.audit.autoconfigure;

import java.time.LocalDateTime;
import java.time.ZoneId;

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
    private final ZoneId zone;

    @Scheduled(cron = "${framework.audit-events.retention.cron:0 0 3 * * *}")
    public void purge() {
        LocalDateTime cutoff = LocalDateTime.now(zone).minus(props.getMaxAge());
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
