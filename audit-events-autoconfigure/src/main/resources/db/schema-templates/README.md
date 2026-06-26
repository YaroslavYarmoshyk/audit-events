# Audit events – schema templates

Copy-ready database migrations that create the `audit_events` table (and its indexes) for the
JDBC store. Use these in **production** instead of the runtime `schema-init` option, which is a
best-effort, dev-only convenience.

These files are **not** applied automatically. They live outside the default Flyway
(`db/migration`) and Liquibase (`db/changelog`) locations on purpose, so they never run unless you
copy them into your project.

## Flyway

Pick the script for your database and copy it into your Flyway location
(default `src/main/resources/db/migration`), renumbering the `V1__` prefix to fit your history:

| Database | Template |
|---|---|
| PostgreSQL | `flyway/postgresql/V1__create_audit_events_table.sql` |
| MySQL / MariaDB | `flyway/mysql/V1__create_audit_events_table.sql` |
| SQL Server | `flyway/sqlserver/V1__create_audit_events_table.sql` |

## Liquibase

Copy `liquibase/audit-events.changelog.yaml` into your project and include it from your master
changelog:

```yaml
- include:
    file: db/changelog/audit-events.changelog.yaml
```

A single changeset covers all vendors – Liquibase maps the generic column types per database.

## Schema contract

The store reads and writes exactly these columns, so keep them as-is:

| Column | Type (portable) | Notes |
|---|---|---|
| `id` | `VARCHAR(36)` | primary key (UUID string) |
| `type` | `VARCHAR(128)` | event type, `NOT NULL` |
| `metadata` | text / `CLOB` | nullable; raw string or JSON |
| `created_by` | `VARCHAR(255)` | actor, `NOT NULL` |
| `created_at` | `TIMESTAMP(6)` | `NOT NULL` |

Indexes target the query patterns (filter by `type` / `created_by`, ordered/ranged by
`created_at`) and the retention purge (`created_at`). If you rename the table via
`framework.audit-events.storage.jdbc.table-name`, rename it in the migration too.
