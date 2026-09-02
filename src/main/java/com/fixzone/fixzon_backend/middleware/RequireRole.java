package com.fixzone.fixzon_backend.middleware;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to specify required user roles for accessing a controller or specific endpoint.
 * Can be placed at class level (applies to all methods in controller) or method level (overrides class level).
 * 
 * Example usage:
 * @RequireRole({"ROLE_COMPANY_OWNER", "ROLE_SUPER_ADMIN"})
 * or
 * @RequireRole({"ROLE_SUPER_ADMIN"})
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    String[] value();
}
