package com.acme.audit.autoconfigure;

import java.lang.reflect.Method;

import com.acme.audit.AuditEventPublisher;
import com.acme.audit.Audited;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jspecify.annotations.Nullable;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.StringUtils;

/**
 * Records an audit event after an {@link Audited @Audited} method returns successfully.
 * The optional {@code metadata} attribute is evaluated as SpEL against the method
 * arguments and {@code #result}.
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AuditedAspect {
    private final AuditEventPublisher publisher;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer paramNames = new DefaultParameterNameDiscoverer();

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void onAudited(JoinPoint joinPoint, Audited audited, @Nullable Object result) {
        String metadata = evaluateMetadata(audited.metadata(), joinPoint, result);
        publisher.publish(audited.type(), metadata);
    }

    private @Nullable String evaluateMetadata(String expression, JoinPoint joinPoint, @Nullable Object result) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }
        try {
            StandardEvaluationContext context = new StandardEvaluationContext();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            Object[] args = joinPoint.getArgs();
            String[] names = paramNames.getParameterNames(method);
            if (names != null) {
                for (int i = 0; i < names.length; i++) {
                    context.setVariable(names[i], args[i]);
                }
            }
            context.setVariable("result", result);
            Expression parsed = parser.parseExpression(expression);
            Object value = parsed.getValue(context);
            return value != null ? value.toString() : null;
        } catch (RuntimeException ex) {
            log.debug("Failed to evaluate @Audited metadata expression '{}': {}", expression, ex.toString());
            return null;
        }
    }
}
