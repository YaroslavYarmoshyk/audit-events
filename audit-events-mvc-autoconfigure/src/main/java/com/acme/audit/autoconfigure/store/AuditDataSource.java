package com.acme.audit.autoconfigure.store;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Marks the {@code DataSource} the audit library should use in a multi-datasource app:
 *
 * <pre>{@code
 * @Bean @AuditDataSource
 * DataSource auditDataSource() { ... }
 * }</pre>
 *
 * Resolution order: {@code framework.audit-events.storage.jdbc.datasource-bean} -> {@code @AuditDataSource}
 * -> {@code @Primary} -> sole candidate.
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface AuditDataSource {
}
