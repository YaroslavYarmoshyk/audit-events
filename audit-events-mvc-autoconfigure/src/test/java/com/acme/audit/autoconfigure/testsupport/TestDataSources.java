package com.acme.audit.autoconfigure.testsupport;

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

    /** A fresh embedded H2 database with the given (unique) name. */
    public static DataSource h2(String name) {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName(name)
                .build();
    }
}
