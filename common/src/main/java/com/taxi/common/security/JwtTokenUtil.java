package com.taxi.common.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Slf4j
public class JwtTokenUtil {

    private final SecretKey secretKey;
    private final Long accessTokenExpiration;
    private final Long refreshTokenExpiration;

    private final String ACCESS_TOKEN_SUBJECT = "AccessToken";
    private final String REFRESH_TOKEN_SUBJECT = "RefreshToken";
    private final String TOKEN_CLAIM = "email";
    private final String ROLE_CLAIM = "role";

    public JwtTokenUtil(String secretKey, Long accessTokenExpiration, Long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(secretKey));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    // 액세스 토큰 생성
    public String generateAccessToken(String email, String role) {
        if (email == null || role == null) {
            log.error(">>> Invalid User ID or Role <<<");
            throw new IllegalArgumentException("User ID or Role은 필수 입력 값입니다.");
        }

        log.info(">>> Generate Access Token / Email : {} | Role : {} <<<", email, role);

        return Jwts.builder()
                .claim(TOKEN_CLAIM, email)
                .claim(ROLE_CLAIM, role)
                .setSubject(ACCESS_TOKEN_SUBJECT)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + accessTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(String email) {
        if (email == null) {
            throw new IllegalArgumentException("Email은 필수 입력 값입니다.");
        }

        log.info(">>> Generate Refresh Token / Email : {} <<<", email);

        return Jwts.builder()
                .claim(TOKEN_CLAIM, email)
                .setSubject(REFRESH_TOKEN_SUBJECT)
                .setIssuedAt(new Date())
                .setExpiration(new Date(new Date().getTime() + refreshTokenExpiration))
                .signWith(secretKey)
                .compact();
    }

    // 토큰에서 email 파싱
    public String extractEmail(String token) {
        log.info(">>> Extract UserID / JWT Token : {} <<<", token);

        return (String) Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get(TOKEN_CLAIM);
    }

    // 토큰에서 role 파싱
    public String extractRole(String token) {
        log.info(">>> Extract Role / JWT Token : {} <<<", token);

        return (String) Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get(ROLE_CLAIM);
    }

    // 토큰 만료 여부
    public boolean isTokenExpired(String token) {
        log.info(">>> Token Expired Check / JWT Token : {} <<<", token);

        try {
            Date expiration = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();

            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            log.error(">>> Token Is Expired / JWT Token : {} <<<", token);

            return true;
        } catch (Exception e) {
            log.error(">>> Token Is Invalid / JWT Token : {} <<<", token);

            return true;
        }
    }

    // 토큰 만료일
    public Date getTokenExpiration(String token) {
        log.info(">>> Get Token Expiration / JWT Token : {} <<<", token);

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
        } catch (ExpiredJwtException e) {
            log.error(">>> Expired JWT Token / JWT Token : {} <<<", token);

            return null;
        } catch (Exception e) {
            log.error(">>> Invalid JWT Token / JWT Token : {} <<<", token);

            return null;
        }
    }

    //토큰 해쉬처리
    public String tokenToHash(String accessToken) {
        log.info(">>> Token To Hash / JWT Token : {} <<<", accessToken);

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(accessToken.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error(">>> ERROR TO CONVERT HASH / JWT Token : {} <<<", accessToken);

            return null;
        }
    }
}
