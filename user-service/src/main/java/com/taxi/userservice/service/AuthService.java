package com.taxi.userservice.service;

import com.taxi.common.exception.CustomAuthException;
import com.taxi.common.exception.CustomBadRequestException;
import com.taxi.common.exception.CustomInternalException;
import com.taxi.common.security.JwtTokenUtil;
import com.taxi.userservice.dto.LoginRequestDto;
import com.taxi.userservice.dto.TokenDto;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    // 로그인
    public TokenDto login(LoginRequestDto dto) {
        User user = userRepository.findByEmail(dto.getEmail());

        // 회원가입 된 이메일이 아닌 경우
        if (user == null) {
            log.warn(">>> 잘못된 이메일로 로그인 시도 / 입력한 이메일 : {} <<<", dto.getEmail());
            throw new CustomBadRequestException("잘못된 이메일입니다.");
        }

        // 해당 이메일로 회원가입이 되어있지만 SNS 로그인인 경우
        if (!Provider.LOCAL.equals(user.getProvider())) {
            log.warn(">>> 잘못된 로그인 방식으로 로그인 시도 / 이메일 : {} <<<", dto.getEmail());
            throw new CustomBadRequestException("잘못된 로그인 방식입니다. 해당 SNS 로그인을 이용해주세요.");
        }

        // 비밀번호가 맞지 않는 경우
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn(">>> 잘못된 비밀번호로 로그인 시도 / 이메일 : {} <<<", dto.getEmail());
            throw new CustomBadRequestException("잘못된 비밀번호입니다.");
        }

        String accessToken = jwtTokenUtil.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getEmail());

        Date expirationDate = jwtTokenUtil.getTokenExpiration(refreshToken);
        long expiration = expirationDate.getTime() - System.currentTimeMillis();

        redisTemplate.opsForValue().set(user.getEmail(), refreshToken, expiration, TimeUnit.MILLISECONDS);

        return new TokenDto(accessToken, refreshToken);
    }

    // 로그아웃
    public void logout(String accessToken) {
        Date tokenExpiration = jwtTokenUtil.getTokenExpiration(accessToken);

        if (tokenExpiration != null && tokenExpiration.after(new Date())) {
            long expiration = tokenExpiration.getTime() - System.currentTimeMillis();
            String hashAccessToken = jwtTokenUtil.tokenToHash(accessToken);

            redisTemplate.opsForValue().set(hashAccessToken, "logout", expiration, TimeUnit.MILLISECONDS);
        } else {
            log.error(">>> Access Token 만료로 인한 로그아웃 불가능 상태 <<<");
            throw new CustomAuthException("로그아웃 할 수 없는 상태입니다.");
        }
    }

    // 토큰 재발급
    public TokenDto reIssueAccessToken(String refreshToken) {
        String email = jwtTokenUtil.extractEmail(refreshToken);
        String storedRefreshToken = redisTemplate.opsForValue().get(email);

        // 저장된 토큰이 없거나 저장된 토큰과 요청 토큰이 다른 경우
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            log.error(">>> 저장된 토큰과 요청 토큰이 다른 경우 재발급 요청 시도 <<<");
            throw new CustomAuthException("잘못된 토큰 재발급 요청입니다.");
        }

        User user = userRepository.findByEmail(email);
        if (user == null) {
            throw new CustomInternalException("잘못된 내부 오류입니다.");
        }

        String newAccessToken = jwtTokenUtil.generateAccessToken(email, user.getRole().name());

        TokenDto tokenDto = new TokenDto();
        tokenDto.setAccessToken(newAccessToken);

        return tokenDto;
    }
}