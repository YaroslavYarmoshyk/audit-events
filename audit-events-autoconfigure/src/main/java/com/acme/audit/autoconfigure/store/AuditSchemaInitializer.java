package com.acme.audit.autoconfigure.store;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Best-effort schema initialization for the JDBC store (dev/demo convenience). Detects the
 * database vendor from the connection metadata and runs the dedicated CREATE TABLE script for
 * MS SQL Server, PostgreSQL or MySQL, falling back to a portable ANSI script otherwise.
 * Production schemas belong in Flyway/Liquibase.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditSchemaInitializer {
    /** Token replaced with the validated table name in every vendor script. */
    private static final String TABLE_TOKEN = "${audit_table}";

    public static void initialize(DataSource dataSource, String tableName) {
        String table = SqlIdentifier.validate(tableName);
        AuditDatabaseType type = detectType(dataSource);
        String ddl = loadScript(type.schemaScript()).replace(TABLE_TOKEN, table);
        log.debug("Initializing audit schema for table '{}' using {} script", table, type);
        JdbcClient client = JdbcClient.create(dataSource);
        // Scripts may contain several statements (CREATE TABLE + CREATE INDEX); run them one by one.
        for (String statement : ddl.split(";")) {
            String sql = statement.strip();
            if (!sql.isEmpty()) {
                client.sql(sql).update();
            }
        }
    }

    private static AuditDatabaseType detectType(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            AuditDatabaseType type = AuditDatabaseType.fromProductName(product);
            log.debug("Detected database product '{}' -> {}", product, type);
            return type;
        } catch (SQLException ex) {
            log.warn("Could not detect database product, falling back to generic schema: {}", ex.toString());
            return AuditDatabaseType.GENERIC;
        }
    }

    private static String loadScript(String location) {
        try {
            return new ClassPathResource(location).getContentAsString(StandardCharsets.UTF_8).strip();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load audit schema script '" + location + "'", ex);
        }
    }
}
