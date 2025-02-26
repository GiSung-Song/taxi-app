package com.taxi.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    private final List<String> excludeUrls = List.of(
            "/api/user/register",           // 일반 회원가입
            "/api/user/oauth/register",     // SNS 회원가입
            "/api/auth/login",              // 일반 로그인
            "/api/auth/logout",             // 로그아웃
            "/api/auth/refresh",            // 토큰 재발급
            "/oauth2/authorization/kakao",  // 카카오 로그인 시작
            "/oauth2/authorization/naver",  // 네이버 로그인 시작
            "/oauth2/authorization/google", // 구글 로그인 시작
            "/api/auth/success"             // SNS 로그인 성공 처리
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info(">>> 서비스의 Token Filter 실행 <<<");

        if (excludeUrls.stream().anyMatch(request.getRequestURI()::equals)) {
            log.info(">>> Pass Jwt Filter Path : {} <<<", request.getRequestURI());

            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION).substring(7);

        String email = jwtTokenUtil.extractEmail(accessToken);
        String role = jwtTokenUtil.extractRole(accessToken);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}