package com.acme.audit.autoconfigure.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import com.acme.audit.spi.AuditEventStore;
import com.acme.audit.spi.AuditSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Read-side service (Controller -> Service -> Repository/Store). Keeps the controller thin and the
 * store free of DTO concerns, and owns the query semantics - including turning the {@code from}/{@code to}
 * calendar dates into an inclusive date-time range.
 */
@RequiredArgsConstructor
public class AuditQueryService {
    private final AuditEventStore store;

    public Page<AuditEventResponse> search(@Nullable Set<String> types,
                                           @Nullable Set<String> creators,
                                           @Nullable LocalDate from,
                                           @Nullable LocalDate to,
                                           Pageable pageable) {
        AuditSearchCriteria criteria = new AuditSearchCriteria(types, creators, startOfDay(from), endOfDay(to));
        return store.search(criteria, pageable).map(AuditEventResponse::from);
    }

    /** Inclusive lower bound: the first instant of {@code date}. */
    private @Nullable LocalDateTime startOfDay(@Nullable LocalDate date) {
        return (date != null) ? date.atStartOfDay() : null;
    }

    /** Inclusive upper bound: the last instant of {@code date}. */
    private @Nullable LocalDateTime endOfDay(@Nullable LocalDate date) {
        return (date != null) ? date.atTime(LocalTime.MAX) : null;
    }
}
