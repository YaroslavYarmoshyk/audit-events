CREATE TABLE IF NOT EXISTS ${audit_table} (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    type        VARCHAR(128) NOT NULL,
    metadata    TEXT,
    created_by  VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP(6) NOT NULL
);
CREATE INDEX IF NOT EXISTS ix_${audit_table}_type_created_at ON ${audit_table} (type, created_at);
CREATE INDEX IF NOT EXISTS ix_${audit_table}_created_by_created_at ON ${audit_table} (created_by, created_at);
CREATE INDEX IF NOT EXISTS ix_${audit_table}_created_at ON ${audit_table} (created_at);