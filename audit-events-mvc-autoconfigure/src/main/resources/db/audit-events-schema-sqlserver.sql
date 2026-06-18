IF OBJECT_ID(N'${audit_table}', N'U') IS NULL
CREATE TABLE ${audit_table} (
    id          VARCHAR(36)   NOT NULL PRIMARY KEY,
    type        VARCHAR(128)  NOT NULL,
    metadata    NVARCHAR(MAX),
    created_by  VARCHAR(255)  NOT NULL,
    created_at  DATETIME2(6)  NOT NULL,
    INDEX ix_${audit_table}_type_created_at NONCLUSTERED (type, created_at),
    INDEX ix_${audit_table}_created_by_created_at NONCLUSTERED (created_by, created_at),
    INDEX ix_${audit_table}_created_at NONCLUSTERED (created_at)
)