package com.travelingdog.backend.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import com.travelingdog.backend.model.User;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        User user = new User();
        user.setId(1L);
        user.setEmail(annotation.email());
        user.setPassword(annotation.password());
        user.setNickname(annotation.nickname());

        Set<String> roles = new HashSet<>();
        Arrays.stream(annotation.roles())
                .map(role -> "ROLE_" + role)
                .forEach(roles::add);
        user.setRoles(roles);

        Authentication auth = new UsernamePasswordAuthenticationToken(user, user.getPassword(), user.getAuthorities());
        context.setAuthentication(auth);
        return context;
    }
}