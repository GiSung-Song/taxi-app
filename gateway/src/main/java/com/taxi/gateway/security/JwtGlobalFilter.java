package com.taxi.gateway.security;

import com.taxi.common.security.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtTokenUtil jwtTokenUtil;
    private final RedisTemplate<String, String> redisTemplate;

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
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info(">>> Jwt Global Filter 진행 / URI : {} <<<", exchange.getRequest().getURI());

        String path = exchange.getRequest().getURI().getPath();

        // 회원가입, 로그인, 로그아웃 관련 url
        if (excludeUrls.stream().anyMatch(path::equals)) {
            return chain.filter(exchange);
        }

        String accessToken = extractToken(exchange.getRequest().getHeaders());

        if (accessToken == null || jwtTokenUtil.isTokenExpired(accessToken)) {
            log.error(">>> Access Token 유효하지 않음 <<<");

            return onError(exchange, "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }

        if (isTokenLogout(accessToken)) {
            log.error(">>> 로그아웃 처리된 Access Token <<<");

            return onError(exchange, "로그아웃 처리된 토큰입니다.", HttpStatus.UNAUTHORIZED);
        }

        String email = jwtTokenUtil.extractEmail(accessToken);
        String role = jwtTokenUtil.extractRole(accessToken);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                email,
                null,
                List.of(new SimpleGrantedAuthority(role))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        exchange = exchange.mutate()
                .request(r -> r.headers(headers -> headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)))
                .build();

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }

    // 헤더에서 토큰 추출
    private String extractToken(HttpHeaders httpHeaders) {
        String authorizeHeader = httpHeaders.getFirst(HttpHeaders.AUTHORIZATION);

        if (authorizeHeader != null && authorizeHeader.startsWith("Bearer ")) {
            return authorizeHeader.substring(7);
        }

        return null;
    }

    // 오류처리 (401, 403)
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus httpStatus) {
        log.info(">>> 오류 응답 처리 <<<");

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorResponse = String.format("{\"status\" : %d, \"message\" : %s}", httpStatus.value(), message);
        DataBuffer buffer = response.bufferFactory().wrap(errorResponse.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    // 로그아웃 여부
    private boolean isTokenLogout(String accessToken) {
        String hashToken = jwtTokenUtil.tokenToHash(accessToken);

        return redisTemplate.opsForValue().get(hashToken) != null;
    }
}
