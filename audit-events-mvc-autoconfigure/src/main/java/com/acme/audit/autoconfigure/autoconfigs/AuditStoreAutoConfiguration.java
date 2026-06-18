package com.acme.audit.autoconfigure.autoconfigs;

import javax.sql.DataSource;

import com.acme.audit.autoconfigure.AuditProperties;
import com.acme.audit.autoconfigure.store.AuditDataSourceResolver;
import com.acme.audit.autoconfigure.store.AuditSchemaInitializer;
import com.acme.audit.autoconfigure.store.InMemoryAuditEventStore;
import com.acme.audit.autoconfigure.store.JdbcAuditEventStore;
import com.acme.audit.autoconfigure.store.SqlIdentifier;
import com.acme.audit.spi.AuditEventStore;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Selects the {@link AuditEventStore} implementation from {@code framework.audit-events.storage.type}.
 * Defaults to the in-memory store so the app starts with no database. A client-provided
 * {@code AuditEventStore} bean overrides both via {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "framework.audit-events", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditStoreAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(prefix = "framework.audit-events.storage", name = "type", havingValue = "in-memory",
            matchIfMissing = true)
    static class InMemoryStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean(AuditEventStore.class)
        AuditEventStore inMemoryAuditEventStore(AuditProperties properties) {
            return new InMemoryAuditEventStore(properties.getStorage().getInMemory().getCapacity());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JdbcClient.class)
    @ConditionalOnProperty(prefix = "framework.audit-events.storage", name = "type", havingValue = "jdbc")
    static class JdbcStoreConfiguration {

        @Bean
        @ConditionalOnMissingBean(AuditEventStore.class)
        AuditEventStore jdbcAuditEventStore(AuditProperties properties, ConfigurableListableBeanFactory beanFactory) {
            AuditProperties.Storage.Jdbc jdbc = properties.getStorage().getJdbc();
            DataSource dataSource = AuditDataSourceResolver.resolve(jdbc, beanFactory);
            String table = SqlIdentifier.validate(jdbc.getTableName());
            if (jdbc.isSchemaInit()) {
                AuditSchemaInitializer.initialize(dataSource, table);
            }
            return new JdbcAuditEventStore(JdbcClient.create(dataSource), table);
        }
    }
}
