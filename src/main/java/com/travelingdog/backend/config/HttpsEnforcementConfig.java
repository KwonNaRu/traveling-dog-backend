package com.travelingdog.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class HttpsEnforcementConfig {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> httpsEnforcementFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {

                // X-Forwarded-Proto 헤더를 확인하여 HTTP 요청을 HTTPS로 리다이렉트
                String forwardedProto = request.getHeader("X-Forwarded-Proto");

                if (forwardedProto != null && forwardedProto.equals("http")) {
                    String requestURL = request.getRequestURL().toString();
                    String httpsURL = requestURL.replace("http://", "https://");
                    response.sendRedirect(httpsURL);
                    return;
                }

                filterChain.doFilter(request, response);
            }
        });

        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }
}