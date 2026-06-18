package com.acme.audit.autoconfigure.api;

import java.time.LocalDate;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single, flexible query endpoint that subsumes every type/creator/period combination.
 * Disabled by default; enable with {@code framework.audit-events.api.enabled=true}.
 *
 * <p>Authorization is delegated to the application's {@code SecurityFilterChain} matching
 * {@code framework.audit-events.api.base-path} - the idiomatic Spring Security approach for a
 * library endpoint (avoids hard-coupling the library to method security).
 */
@RestController
@RequestMapping("${framework.audit-events.api.base-path:/audit-events}")
@Slf4j
@RequiredArgsConstructor
public class AuditQueryController {
    private final AuditQueryService service;

    @GetMapping
    public PagedModel<AuditEventResponse> search(
            @RequestParam(name = "type", required = false) @Nullable Set<String> types,
            @RequestParam(name = "creator", required = false) @Nullable Set<String> creators,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) @Nullable LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int pageSize = Math.max(1, size);
        log.debug("Audit query endpoint invoked types: {} creators: {} from: {} to: {} page: {} size: {}",
                types, creators, from, to, page, pageSize);

        Pageable pageable = PageRequest.of(Math.max(0, page), pageSize);
        return new PagedModel<>(service.search(types, creators, from, to, pageable));
    }
}
