package com.travelingdog.backend.config;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.travelingdog.backend.exception.UnauthorizedException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@ConditionalOnProperty(name = "rate-limiting.enabled", havingValue = "true")
public class RateLimitingConfig {

    // IP 주소별 RateLimiter를 저장하는 캐시
    private LoadingCache<String, RateLimiter> rateLimiters = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String key) {
                    // 초당 10개의 요청 허용 (조정 가능)
                    return RateLimiter.create(10.0);
                }
            });

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> rateLimitingFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {

                // 클라이언트 IP 주소 가져오기
                String clientIp = getClientIp(request);

                try {
                    // 해당 IP의 RateLimiter 가져오기
                    RateLimiter rateLimiter = rateLimiters.get(clientIp);

                    // 요청 허용 여부 확인
                    if (rateLimiter.tryAcquire()) {
                        // 허용된 경우 요청 처리
                        filterChain.doFilter(request, response);
                    } else {
                        // 허용되지 않은 경우 429 Too Many Requests 응답
                        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                        response.getWriter().write("Too many requests. Please try again later.");
                    }
                } catch (UnauthorizedException e) {
                    // 인증 관련 예외는 상위로 전파
                    throw e;
                } catch (Exception e) {
                    // 다른 예외 발생 시 요청 처리 (Rate Limiting 실패 시에도 서비스는 계속 동작)
                    filterChain.doFilter(request, response);
                }
            }
        });

        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 1); // HTTPS 필터 다음 순서
        registrationBean.addUrlPatterns("/api/*"); // API 경로에만 적용
        return registrationBean;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For 헤더가 있는 경우 첫 번째 IP 사용
            return xForwardedFor.split(",")[0].trim();
        }
        // 없는 경우 원격 주소 사용
        return request.getRemoteAddr();
    }
}