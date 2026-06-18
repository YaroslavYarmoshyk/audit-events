package com.acme.audit.autoconfigure.store;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.acme.audit.AuditEvent;
import com.acme.audit.spi.AuditEventStore;
import com.acme.audit.spi.AuditSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;

import static java.util.Collections.nCopies;

/**
 * JDBC-backed store using {@link JdbcClient}. Uses only ANSI SQL constructs and binds
 * the configurable table name as a validated identifier, so it is not coupled to any
 * specific database vendor.
 */
public class JdbcAuditEventStore implements AuditEventStore {
    private final JdbcClient jdbc;
    private final String table;

    public JdbcAuditEventStore(JdbcClient jdbc, String tableName) {
        this.jdbc = jdbc;
        this.table = SqlIdentifier.validate(tableName);
    }

    @Override
    public void save(AuditEvent event) {
        jdbc.sql("INSERT INTO " + table + " (id, type, metadata, created_by, created_at) "
                        + "VALUES (?, ?, ?, ?, ?)")
                .param(event.id().toString())
                .param(event.type())
                .param(event.metadata())
                .param(event.createdBy())
                .param(Timestamp.from(event.createdAt()))
                .update();
    }

    @Override
    public Page<AuditEvent> search(AuditSearchCriteria criteria, Pageable pageable) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> args = new ArrayList<>();
        Set<String> types = criteria.types();
        if (types != null && !types.isEmpty()) {
            where.append(" AND type IN (").append(placeholders(types.size())).append(')');
            args.addAll(types);
        }
        Set<String> creators = criteria.creators();
        if (creators != null && !creators.isEmpty()) {
            where.append(" AND created_by IN (").append(placeholders(creators.size())).append(')');
            args.addAll(creators);
        }
        if (criteria.from() != null) {
            where.append(" AND created_at >= ?");
            args.add(Timestamp.from(criteria.from()));
        }
        if (criteria.to() != null) {
            where.append(" AND created_at <= ?");
            args.add(Timestamp.from(criteria.to()));
        }

        Long total = jdbc.sql("SELECT COUNT(*) FROM " + table + where)
                .params(args).query(Long.class).single();

        List<AuditEvent> rows = jdbc.sql("SELECT id, type, metadata, created_by, created_at FROM " + table
                        + where + " ORDER BY created_at DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY")
                .params(args)
                .param(pageable.getOffset())
                .param(pageable.getPageSize())
                .query((resultSet, rowNum) -> new AuditEvent(
                        UUID.fromString(resultSet.getString("id")),
                        resultSet.getString("type"),
                        resultSet.getString("metadata"),
                        resultSet.getString("created_by"),
                        resultSet.getTimestamp("created_at").toInstant()))
                .list();

        return new PageImpl<>(rows, pageable, total);
    }

    @Override
    public int deleteOlderThan(Instant cutoff, int batchSize) {
        // Portable two-step: select a bounded batch of ids, then delete them.
        List<String> ids = jdbc.sql("SELECT id FROM " + table
                        + " WHERE created_at < ? ORDER BY created_at OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY")
                .param(Timestamp.from(cutoff))
                .param(batchSize)
                .query(String.class)
                .list();
        if (ids.isEmpty()) {
            return 0;
        }
        return jdbc.sql("DELETE FROM " + table + " WHERE id IN (" + placeholders(ids.size()) + ")")
                .params(new ArrayList<Object>(ids))
                .update();
    }

    private static String placeholders(int count) {
        return String.join(", ", nCopies(count, "?"));
    }
}
