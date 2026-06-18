package com.acme.audit.autoconfigure.store;

import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;

import com.acme.audit.autoconfigure.AuditProperties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringUtils;

/**
 * Resolves the {@link DataSource} the JDBC store should use, failing fast with an
 * actionable message when the configuration is ambiguous or wrong.
 *
 * <p>Resolution order: explicit {@code datasource-bean} -> {@code @AuditDataSource}
 * -> sole candidate / {@code @Primary}.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuditDataSourceResolver {

    public static DataSource resolve(AuditProperties.Storage.Jdbc jdbc, ConfigurableListableBeanFactory beanFactory) {
        // 1) Explicit bean name
        String beanName = jdbc.getDatasourceBean();
        if (StringUtils.hasText(beanName)) {
            try {
                DataSource dataSource = beanFactory.getBean(beanName, DataSource.class);
                log.debug("Audit JDBC store using DataSource bean '{}'", beanName);
                return dataSource;
            } catch (NoSuchBeanDefinitionException ex) {
                throw new IllegalStateException("framework.audit-events.storage.jdbc.datasource-bean='" + beanName
                        + "' not found. Available DataSource beans: " + dataSourceNames(beanFactory), ex);
            }
        }

        Map<String, DataSource> candidates = beanFactory.getBeansOfType(DataSource.class);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("framework.audit-events.storage.type=jdbc but no DataSource bean exists. "
                    + "Define a DataSource or set framework.audit-events.storage.type=in-memory.");
        }

        // 2) @AuditDataSource-annotated bean
        for (String name : candidates.keySet()) {
            if (beanFactory.findAnnotationOnBean(name, AuditDataSource.class) != null) {
                log.debug("Audit JDBC store using @AuditDataSource bean '{}'", name);
                return candidates.get(name);
            }
        }

        // 3) sole candidate
        if (candidates.size() == 1) {
            return candidates.values().iterator().next();
        }

        // 4) @Primary
        try {
            DataSource primary = beanFactory.getBean(DataSource.class);
            log.debug("Audit JDBC store using @Primary DataSource");
            return primary;
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Multiple DataSource beans found " + candidates.keySet()
                    + " and none is @Primary or @AuditDataSource. "
                    + "Set framework.audit-events.storage.jdbc.datasource-bean to choose one.", ex);
        }
    }

    private static Set<String> dataSourceNames(ConfigurableListableBeanFactory beanFactory) {
        return beanFactory.getBeansOfType(DataSource.class).keySet();
    }
}
