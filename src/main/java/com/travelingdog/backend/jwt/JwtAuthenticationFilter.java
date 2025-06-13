package com.travelingdog.backend.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.travelingdog.backend.exception.ExpiredJwtException;
import com.travelingdog.backend.exception.InvalidJwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            boolean isPublicEndpoint = isPublicEndpoint(request);
            String token = extractToken(request, isPublicEndpoint);

            // 토큰이 있으면 인증 처리
            if (token != null) {
                jwtTokenProvider.validateToken(token);
                processAuthentication(token, request);
            }

            filterChain.doFilter(request, response);
        } catch (InvalidJwtException | ExpiredJwtException e) {
            handleAuthenticationException(request, response, e);
        } catch (Exception e) {
            handleAuthenticationException(request, response,
                    new InsufficientAuthenticationException("인증 처리 중 오류 발생", e));
        }
    }

    private boolean isPublicEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/auth/")
                || path.equals("/api/travel/plan/recent")
                || path.equals("/api/travel/plan/popular")
                || path.matches("/api/travel/plan/\\d+");
    }

    private String extractToken(HttpServletRequest request, boolean isPublicEndpoint) {
        String clientType = request.getHeader("X-Client-Type");
        boolean isAppRequest = "APP".equals(clientType);

        if (isAppRequest) {
            return handleAppRequest(request, isPublicEndpoint);
        } else {
            return handleWebRequest(request, isPublicEndpoint);
        }
    }

    private String handleAppRequest(HttpServletRequest request, boolean isPublicEndpoint) {
        String token = extractBearerToken(request);
        if (token == null && !isPublicEndpoint) {
            throw new InvalidJwtException("앱 API는 Bearer 토큰이 필요합니다.");
        }
        return token;
    }

    private String handleWebRequest(HttpServletRequest request, boolean isPublicEndpoint) {
        String token = extractCookieToken(request);
        // Bearer 토큰 사용 시도를 차단 (공개 API는 제외)
        if (extractBearerToken(request) != null && !isPublicEndpoint) {
            throw new InvalidJwtException("웹 API는 쿠키 기반 인증만 허용됩니다.");
        }
        return token;
    }

    private void handleAuthenticationException(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException e) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        jwtAuthenticationEntryPoint.commence(request, response, e);
    }

    // Bearer 토큰 추출 (앱용)
    private String extractBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 쿠키 토큰 추출 (웹용)
    private String extractCookieToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    // 인증 처리 로직
    private void processAuthentication(String token, HttpServletRequest request) {
        String email = jwtTokenProvider.extractEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 공개 API는 필터를 거치지 않음
        return isPublicEndpoint(request);
    }
}