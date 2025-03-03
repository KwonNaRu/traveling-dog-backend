package com.travelingdog.backend.config;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.security.test.context.support.WithSecurityContext;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
    String email() default "test@example.com";

    String password() default "password";

    String nickname() default "Test User";

    String[] roles() default { "USER" };
}