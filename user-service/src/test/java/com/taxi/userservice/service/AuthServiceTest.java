package com.taxi.userservice.service;

import com.taxi.common.core.exception.CustomAuthException;
import com.taxi.common.core.exception.CustomBadRequestException;
import com.taxi.common.security.JwtTokenUtil;
import com.taxi.userservice.dto.LoginRequestDto;
import com.taxi.userservice.dto.TokenDto;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ActiveProfiles("h2")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void 로그인_성공_테스트() {
        User user = User.builder()
                .name("테스트1")
                .email("test@test.com")
                .role(Role.USER)
                .password("encodedPassword")
                .provider(Provider.LOCAL)
                .build();

        String accessToken = "accessToken";
        String refreshToken = "refreshToken";

        Date expirationDate = new Date(System.currentTimeMillis() + 1000L * 60 * 60);

        when(userRepository.findByEmail(anyString()))
                .thenReturn(user);
        when(jwtTokenUtil.generateAccessToken(user.getEmail(), user.getRole().name())).thenReturn(accessToken);
        when(jwtTokenUtil.generateRefreshToken(user.getEmail())).thenReturn(refreshToken);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(jwtTokenUtil.getTokenExpiration(refreshToken)).thenReturn(expirationDate);

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("test@test.com");
        dto.setPassword("password");

        TokenDto token = authService.login(dto);

        assertEquals(accessToken, token.getAccessToken());
        assertEquals(refreshToken, token.getRefreshToken());
        verify(redisTemplate.opsForValue(), times(1)).set(eq(user.getEmail()), eq(refreshToken),
                anyLong(), any());
    }

    @Test
    void 로그인_실패_테스트_없는_아이디() {
        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("test@test.com");
        dto.setPassword("password");

        assertThrows(CustomBadRequestException.class, () -> authService.login(dto));
    }

    @Test
    void 로그인_실패_테스트_잘못된_SNS_로그인() {
        User user = User.builder()
                .name("테스트1")
                .email("test@test.com")
                .role(Role.USER)
                .password("encodedPassword")
                .provider(Provider.GOOGLE)
                .build();

        when(userRepository.findByEmail(anyString()))
                .thenReturn(user);

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("test@test.com");
        dto.setPassword("password");

        assertThrows(CustomBadRequestException.class, () -> authService.login(dto));
    }

    @Test
    void 로그인_실패_테스트_잘못된_패스워드() {
        User user = User.builder()
                .name("테스트1")
                .email("test@test.com")
                .role(Role.USER)
                .password("encodedPassword")
                .provider(Provider.LOCAL)
                .build();

        when(userRepository.findByEmail(anyString()))
                .thenReturn(user);

        LoginRequestDto dto = new LoginRequestDto();
        dto.setEmail("test@test.com");
        dto.setPassword("password");
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(false);

        assertThrows(CustomBadRequestException.class, () -> authService.login(dto));
    }

    @Test
    void 로그아웃_성공_테스트() {
        String accessToken = "accessToken";
        Date expirationDate = new Date(System.currentTimeMillis() + 1000L * 60 * 60);

        when(jwtTokenUtil.getTokenExpiration(accessToken)).thenReturn(expirationDate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtTokenUtil.tokenToHash(accessToken)).thenReturn("hashToken");

        authService.logout(accessToken);

        verify(redisTemplate.opsForValue(), times(1)).set(eq("hashToken"), eq("logout"), anyLong(), any());
    }

    @Test
    void 로그아웃_실패_테스트() {
        String accessToken = "accessToken";
        Date expirationDate = new Date();

        when(jwtTokenUtil.getTokenExpiration(accessToken)).thenReturn(expirationDate);

        assertThrows(CustomAuthException.class, () -> authService.logout(accessToken));
    }

    @Test
    void 토큰_재발급_성공_테스트() {
        User user = User.builder()
                .id(1L)
                .name("테스트1")
                .email("test@test.com")
                .role(Role.USER)
                .password("encodedPassword")
                .provider(Provider.LOCAL)
                .build();

        String refreshToken = "refreshToken";
        String storedRefreshToken = "refreshToken";
        String newAccessToken = "accessToken";

        when(jwtTokenUtil.extractEmail(refreshToken)).thenReturn(user.getEmail());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(user.getEmail())).thenReturn(storedRefreshToken);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);
        when(jwtTokenUtil.generateAccessToken(user.getEmail(), user.getRole().name())).thenReturn(newAccessToken);

        TokenDto tokenDto = authService.reIssueAccessToken(refreshToken);

        assertEquals("accessToken", tokenDto.getAccessToken());
    }

    @Test
    void 토큰_재발급_실패_테스트() {
        User user = User.builder()
                .id(1L)
                .name("테스트1")
                .email("test@test.com")
                .role(Role.USER)
                .password("encodedPassword")
                .provider(Provider.LOCAL)
                .build();

        String refreshToken = "refreshToken";
        String storedRefreshToken = "storedRefreshToken";

        when(jwtTokenUtil.extractEmail(refreshToken)).thenReturn(user.getEmail());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(user.getEmail())).thenReturn(storedRefreshToken);

        assertThrows(CustomAuthException.class, () -> authService.reIssueAccessToken(refreshToken));
    }
}