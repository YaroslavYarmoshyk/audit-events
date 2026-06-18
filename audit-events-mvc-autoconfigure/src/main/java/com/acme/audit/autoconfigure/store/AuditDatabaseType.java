package com.acme.audit.autoconfigure.store;

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Recognized database vendors for best-effort schema initialization. Each value points at a
 * dedicated DDL script on the classpath. Anything that is not one of the three explicitly
 * supported vendors (MS SQL Server, PostgreSQL, MySQL) falls back to a portable ANSI script.
 */
public enum AuditDatabaseType {
    POSTGRESQL("db/audit-events-schema-postgresql.sql"),
    MYSQL("db/audit-events-schema-mysql.sql"),
    SQLSERVER("db/audit-events-schema-sqlserver.sql"),
    GENERIC("db/audit-events-schema-default.sql");

    private final String schemaScript;

    AuditDatabaseType(String schemaScript) {
        this.schemaScript = schemaScript;
    }

    /** Classpath location of the dedicated CREATE TABLE script for this vendor. */
    public String schemaScript() {
        return schemaScript;
    }

    /**
     * Maps a JDBC {@code DatabaseMetaData.getDatabaseProductName()} to a known vendor.
     * Returns {@link #GENERIC} when the product is null or unrecognized.
     */
    public static AuditDatabaseType fromProductName(@Nullable String productName) {
        if (productName == null) {
            return GENERIC;
        }
        String product = productName.toLowerCase(Locale.ROOT);
        if (product.contains("postgresql")) {
            return POSTGRESQL;
        }
        if (product.contains("mysql")) {
            return MYSQL;
        }
        if (product.contains("microsoft sql server")) {
            return SQLSERVER;
        }
        return GENERIC;
    }
}
