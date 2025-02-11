package com.taxi.common.security;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenUtilTest {

    private final String secretKey = "testSecretKey1234u0fsjiasdij10j320ujfas083h023hifdhi2dsi2";
    private final Long accessTokenExpiration = 1000 * 60 * 1L; // 1분
    private final Long refreshTokenExpiration = 1000 * 60 * 3L; // 3분

    private final JwtTokenUtil jwtTokenUtil = new JwtTokenUtil(secretKey, accessTokenExpiration, refreshTokenExpiration);

    @Test
    void 액세스_토큰_생성_테스트() {
        Long userId = 1L;
        String role = "DRIVER";

        String token = jwtTokenUtil.generateAccessToken(userId, role);

        assertNotNull(token);
    }

    @Test
    void 액세스_토큰_생성_실패_테스트() {
        assertThrows(IllegalArgumentException.class, () -> jwtTokenUtil.generateAccessToken(null, "DRIVER"));
    }

    @Test
    void 리프레시_토큰_생성_테스트() {
        String token = jwtTokenUtil.generateRefreshToken(0L);

        assertNotNull(token);
    }

    @Test
    void 리프레시_토큰_생성_실패_테스트() {
        assertThrows(IllegalArgumentException.class, () -> jwtTokenUtil.generateRefreshToken(null));
    }

    @Test
    void User_ID_추출_테스트() {
        String token = jwtTokenUtil.generateAccessToken(1L, "RIDER");

        Long extractUserId = jwtTokenUtil.extractUserId(token);

        assertEquals(1L, extractUserId);
    }

    @Test
    void Role_추출_테스트() {
        String token = jwtTokenUtil.generateAccessToken(1L, "RIDER");

        String extractRole = jwtTokenUtil.extractRole(token);

        assertEquals("RIDER", extractRole);
    }

    @Test
    void 만료_여부_체크_테스트() {
        String token = jwtTokenUtil.generateAccessToken(1L, "RIDER");

        boolean isTokenExpired = jwtTokenUtil.isTokenExpired(token);

        assertEquals(false, isTokenExpired);
    }

    @Test
    void 만료_여부_체크_테스트2() throws InterruptedException {
        String token = jwtTokenUtil.generateAccessToken(1L, "RIDER");

        Thread.sleep(1000 * 60 * 1L);

        boolean isTokenExpired = jwtTokenUtil.isTokenExpired(token);

        assertEquals(true, isTokenExpired);
    }

    @Test
    void 만료일_조회_테스트() {
        String Atoken = jwtTokenUtil.generateAccessToken(1L, "RIDER");
        String Rtoken = jwtTokenUtil.generateRefreshToken(1L);

        Date ATokenExpired = jwtTokenUtil.getTokenExpiration(Atoken);
        Date RTokenExpired = jwtTokenUtil.getTokenExpiration(Rtoken);

        assertNotNull(ATokenExpired);
        assertNotNull(RTokenExpired);
    }

    @Test
    void 토큰_해쉬_테스트() {
        String token = jwtTokenUtil.generateAccessToken(1L, "RIDER");

        String hashToken = jwtTokenUtil.tokenToHash(token);

        assertNotNull(hashToken);

        String hashToken2 = jwtTokenUtil.tokenToHash(token);

        // 일관된 해쉬 값 체크
        assertEquals(hashToken, hashToken2);
    }
}