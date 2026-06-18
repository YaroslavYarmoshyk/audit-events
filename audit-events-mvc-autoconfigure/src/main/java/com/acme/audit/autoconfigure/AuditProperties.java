package com.acme.audit.autoconfigure;

import java.time.Period;
import java.time.ZoneId;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Complete configuration model for the audit library. Bound from the
 * {@code framework.audit-events.*} namespace in {@code application.yml}.
 */
@Getter
@Setter
@ConfigurationProperties("framework.audit-events")
public class AuditProperties {
    /** Master switch. Set {@code framework.audit-events.enabled=false} per environment for a full no-op. */
    private boolean enabled = true;

    /** Library-wide time zone for the audit {@link java.time.Clock} and calendar-based retention math. */
    private ZoneId zone = ZoneId.of("America/Toronto");

    private final Storage storage = new Storage();
    private final Async async = new Async();
    private final Security security = new Security();
    private final Api api = new Api();
    private final Retention retention = new Retention();

    @Getter
    @Setter
    public static class Storage {
        public enum Type { IN_MEMORY, JDBC }

        private Type type = Type.IN_MEMORY;
        private final InMemory inMemory = new InMemory();
        private final Jdbc jdbc = new Jdbc();

        @Getter
        @Setter
        public static class InMemory {
            /** Maximum number of events kept; oldest are evicted past this bound. */
            private int capacity = 1000;
        }

        @Getter
        @Setter
        public static class Jdbc {
            /** Name of the DataSource bean to use. Empty = qualifier/@Primary resolution. */
            private String datasourceBean = "";
            /** Target table; DB-agnostic, validated as a SQL identifier. */
            private String tableName = "audit_events";
            /** Run the bundled DDL on startup (best-effort, for demos/dev). */
            private boolean schemaInit = false;
        }
    }

    @Getter
    @Setter
    public static class Async {
        public enum Mode { VIRTUAL, PLATFORM }

        /** When false, persistence runs synchronously (handy for tests). */
        private boolean enabled = true;
        private Mode mode = Mode.VIRTUAL;
        /** Pool size used only in PLATFORM mode. */
        private int platformPoolSize = 4;
    }

    @Getter
    @Setter
    public static class Security {
        /**
         * Records a LOGIN on each interactive login ({@code InteractiveAuthenticationSuccessEvent}).
         * Has no effect for stateless (JWT/bearer) flows, which have no interactive login to hook -
         * such applications must record LOGIN themselves (see the README).
         */
        private boolean loginEventsEnabled = true;
        private boolean logoutEventsEnabled = true;
    }

    @Getter
    @Setter
    public static class Api {
        /** REST query API is OFF by default. */
        private boolean enabled = false;
        private String basePath = "/audit-events";
    }

    @Getter
    @Setter
    public static class Retention {
        /** Purging is OFF by default - deleting audit data is a deliberate act. */
        private boolean enabled = false;
        /** Delete rows older than this. Parsed as a Period (e.g. {@code 7y}, {@code 90d}). */
        private Period maxAge = Period.ofYears(7);
        /** Bounded deletes to avoid long locks. */
        private int batchSize = 5000;
        private String cron = "0 0 3 * * *";
    }
}
