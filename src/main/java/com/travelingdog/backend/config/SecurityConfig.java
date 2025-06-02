package com.travelingdog.backend.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.travelingdog.backend.jwt.JwtAuthenticationEntryPoint;
import com.travelingdog.backend.jwt.JwtAuthenticationFilter;
import com.travelingdog.backend.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final UserDetailsService userDetailsService;
        private final JwtTokenProvider jwtTokenProvider;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/api/user/**").authenticated()
                                                .requestMatchers("/api/protected/**").authenticated()
                                                .requestMatchers("/api/travel/**").authenticated()
                                                .requestMatchers("/swagger-ui.html").permitAll()
                                                .requestMatchers("/swagger-ui/**").permitAll()
                                                .requestMatchers("/v3/api-docs/**").permitAll()
                                                .requestMatchers("/swagger-resources/**").permitAll()
                                                .requestMatchers("/webjars/**").permitAll()
                                                .anyRequest().permitAll())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint));

                http.addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService),
                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:3000", "https://*.narudog.com"));

                configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

                configuration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Requested-With",
                                "Accept",
                                "Origin",
                                "Access-Control-Request-Method",
                                "Access-Control-Request-Headers"));

                configuration.setAllowCredentials(true);

                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);

                return source;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
