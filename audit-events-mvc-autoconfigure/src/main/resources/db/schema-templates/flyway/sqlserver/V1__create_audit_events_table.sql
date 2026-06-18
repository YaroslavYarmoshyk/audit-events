-- Flyway migration template for the audit-events table (Microsoft SQL Server).
--
-- Copy this file into your application's Flyway location (default: src/main/resources/db/migration)
-- and renumber the version prefix (V1__) to fit your migration history. Adjust the table name only
-- if you also set framework.audit-events.storage.jdbc.table-name.

CREATE TABLE audit_events (
    id         VARCHAR(36)   NOT NULL CONSTRAINT pk_audit_events PRIMARY KEY,
    type       VARCHAR(128)  NOT NULL,
    metadata   NVARCHAR(MAX),
    created_by VARCHAR(255)  NOT NULL,
    created_at DATETIME2(6)  NOT NULL
);

CREATE INDEX ix_audit_events_type_created_at       ON audit_events (type, created_at);
CREATE INDEX ix_audit_events_created_by_created_at ON audit_events (created_by, created_at);
CREATE INDEX ix_audit_events_created_at            ON audit_events (created_at);
