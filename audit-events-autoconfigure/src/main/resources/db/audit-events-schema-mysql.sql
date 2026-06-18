CREATE TABLE IF NOT EXISTS ${audit_table} (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    type        VARCHAR(128) NOT NULL,
    metadata    TEXT,
    created_by  VARCHAR(255) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    INDEX ix_${audit_table}_type_created_at (type, created_at),
    INDEX ix_${audit_table}_created_by_created_at (created_by, created_at),
    INDEX ix_${audit_table}_created_at (created_at)
)