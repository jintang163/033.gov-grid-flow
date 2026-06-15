package com.gov.grid.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    String module() default "";

    String operation() default "";

    String description() default "";

    boolean recordParams() default true;

    boolean recordResult() default true;
}
