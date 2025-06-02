package com.travelingdog.backend.jwt;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = null;
            String requestPath = request.getHeader("X-Client-Type");

            // URL 패턴으로 앱 요청 구분
            if (requestPath != null && requestPath.equals("APP")) {
                // 앱 요청은 Bearer 토큰만 허용
                token = extractBearerToken(request);
                if (token == null) {
                    throw new InvalidJwtException("앱 API는 Bearer 토큰이 필요합니다.");
                }
            } else {
                // 일반 웹 요청은 쿠키만 허용
                token = extractCookieToken(request);
                // Bearer 토큰으로 시도하는 경우 차단
                if (extractBearerToken(request) != null) {
                    throw new InvalidJwtException("웹 API는 쿠키 기반 인증만 허용됩니다.");
                }
            }

            // 토큰 유효성 검사 및 인증 처리
            if (token != null) {
                // 토큰 검증 - 예외가 발생하면 그대로 전파
                jwtTokenProvider.validateToken(token);

                // 인증 처리
                processAuthentication(token, request);
            }

            filterChain.doFilter(request, response);
        } catch (InvalidJwtException | ExpiredJwtException e) {
            // 예외를 Spring Security가 처리할 수 있는 인증 예외로 변환하여 전달
            SecurityContextHolder.clearContext();
            throw new BadCredentialsException(e.getMessage(), e);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            throw new RuntimeException("인증 처리 중 오류 발생", e);
        }
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
        String path = request.getServletPath();
        return path.startsWith("/api/auth/");
    }
}