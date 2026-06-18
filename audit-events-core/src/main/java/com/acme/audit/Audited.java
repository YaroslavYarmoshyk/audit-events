package com.acme.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Records an audit event when the annotated method returns successfully.
 * Implemented with Spring AOP.
 *
 * <pre>{@code
 * @Audited(type = "ORDER_CANCELLED", metadata = "#order.id")
 * public void cancel(Order order) { ... }
 * }</pre>
 *
 * <p>{@code type} is mandatory. {@code metadata} is optional and may be a SpEL
 * expression evaluated against the method arguments (and {@code #result}).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** Event type code (mandatory). */
    String type();

    /** Optional metadata; supports SpEL, e.g. {@code "#order.id"}. */
    String metadata() default "";
}
