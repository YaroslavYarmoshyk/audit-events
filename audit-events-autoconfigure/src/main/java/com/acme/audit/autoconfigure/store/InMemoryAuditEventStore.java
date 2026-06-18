package com.acme.audit.autoconfigure.store;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.acme.audit.AuditEvent;
import com.acme.audit.spi.AuditEventStore;
import com.acme.audit.spi.AuditSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Default store used when no datasource is configured: a bounded, concurrent map.
 * Oldest events are evicted once {@code capacity} is exceeded. This is analytics-grade
 * storage (not durable).
 */
public class InMemoryAuditEventStore implements AuditEventStore {
    private final int capacity;
    private final ConcurrentHashMap<UUID, AuditEvent> events = new ConcurrentHashMap<>();
    private final ConcurrentLinkedDeque<UUID> order = new ConcurrentLinkedDeque<>();

    public InMemoryAuditEventStore(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    @Override
    public void save(AuditEvent event) {
        events.put(event.id(), event);
        order.addLast(event.id());
        while (events.size() > capacity) {
            UUID oldest = order.pollFirst();
            if (oldest == null) {
                break;
            }
            events.remove(oldest);
        }
    }

    @Override
    public Page<AuditEvent> search(AuditSearchCriteria criteria, Pageable pageable) {
        List<AuditEvent> matches = new ArrayList<>();
        for (AuditEvent event : events.values()) {
            if (matches(event, criteria)) {
                matches.add(event);
            }
        }
        matches.sort(Comparator.comparing(AuditEvent::createdAt).reversed());

        int from = (int) Math.min(pageable.getOffset(), matches.size());
        int to = Math.min(from + pageable.getPageSize(), matches.size());
        List<AuditEvent> pageContent = matches.subList(from, to);
        return new PageImpl<>(pageContent, pageable, matches.size());
    }

    @Override
    public int deleteOlderThan(Instant cutoff, int batchSize) {
        int deleted = 0;
        for (AuditEvent event : events.values()) {
            if (deleted >= batchSize) {
                break;
            }
            if (event.createdAt().isBefore(cutoff) && events.remove(event.id()) != null) {
                order.remove(event.id());
                deleted++;
            }
        }
        return deleted;
    }

    private static boolean matches(AuditEvent event, AuditSearchCriteria criteria) {
        Set<String> types = criteria.types();
        if (types != null && !types.isEmpty() && !types.contains(event.type())) {
            return false;
        }
        Set<String> creators = criteria.creators();
        if (creators != null && !creators.isEmpty() && !creators.contains(event.createdBy())) {
            return false;
        }
        if (criteria.from() != null && event.createdAt().isBefore(criteria.from())) {
            return false;
        }
        return criteria.to() == null || !event.createdAt().isAfter(criteria.to());
    }
}
