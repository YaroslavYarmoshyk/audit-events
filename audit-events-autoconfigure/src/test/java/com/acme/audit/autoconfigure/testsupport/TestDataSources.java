package com.acme.audit.autoconfigure.testsupport;

import java.util.UUID;
import javax.sql.DataSource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Embedded-database fixtures for the JDBC store tests. Kept separate from {@link TestDataFactory}
 * because it references spring-jdbc, which is not on the reactive module's test classpath.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TestDataSources {

    /**
     * A fresh, isolated embedded H2 database. The supplied name is only a readable prefix: a random
     * suffix is appended so every call gets its own in-memory instance, which keeps tests that run
     * concurrently (or reuse the same logical name) from sharing a schema or closing each other's DB.
     */
    public static DataSource h2(String name) {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName(name + "-" + UUID.randomUUID())
                .build();
    }
}
