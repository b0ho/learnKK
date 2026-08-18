package com.learnkk.kernel.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated {@link Principal} into a controller method parameter. Resolved by
 * {@link AuthPrincipalArgumentResolver} from the request attribute set by the session interceptor.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthPrincipal {}
