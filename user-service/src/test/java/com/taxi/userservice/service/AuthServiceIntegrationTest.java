package com.taxi.userservice.service;

import com.taxi.common.exception.CustomAuthException;
import com.taxi.common.exception.CustomBadRequestException;
import com.taxi.common.security.JwtTokenUtil;
import com.taxi.userservice.config.TestContainerConfig;
import com.taxi.userservice.dto.LoginRequestDto;
import com.taxi.userservice.dto.TokenDto;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
public class AuthServiceIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        redisTemplate.delete("test@test.com");

        User user = User.builder()
                .name("tester")
                .email("test@test.com")
                .password(passwordEncoder.encode("password"))
                .phoneNumber("01012341234")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .build();

        userRepository.save(user);
    }

    @Test
    void 로그인_테스트() {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("password");

        TokenDto token = authService.login(dto);

        String refreshToken = redisTemplate.opsForValue().get(dto.getEmail());

        assertThat(token.getAccessToken()).isNotNull();
        assertThat(token.getRefreshToken()).isNotNull();
        assertThat(refreshToken).isEqualTo(token.getRefreshToken());
    }

    @Test
    void 로그인_실패_이메일() {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test2@test.com");
        dto.setPassword("password");

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(CustomBadRequestException.class);
    }

    @Test
    void 로그인_실패_이메일_SNS() {
        User user2 = User.builder()
                .name("tester")
                .email("test2@naver.com")
                .password(passwordEncoder.encode("password"))
                .providerId("asfdjpfdjsap")
                .phoneNumber("01012341235")
                .role(Role.USER)
                .provider(Provider.NAVER)
                .build();

        userRepository.save(user2);

        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test2@naver.com");
        dto.setPassword("password");

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(CustomBadRequestException.class);
    }

    @Test
    void 로그인_실패_비밀번호() {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("passwordd");

        assertThatThrownBy(() -> authService.login(dto))
                .isInstanceOf(CustomBadRequestException.class);
    }

    @Test
    void 로그아웃_테스트() {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("password");

        TokenDto token = authService.login(dto);
        authService.logout(token.getAccessToken());

        String hashToken = jwtTokenUtil.tokenToHash(token.getAccessToken());
        String isLogout = redisTemplate.opsForValue().get(hashToken);

        assertThat(isLogout).isEqualTo("logout");
    }

    @Test
    void 로그아웃_실패_테스트() {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("password");

        TokenDto token = authService.login(dto);

        assertThatThrownBy(() -> authService.logout("132421580.53421543.fdsafdsa"))
                .isInstanceOf(CustomAuthException.class);
    }

    @Test
    void 토큰_재발급_테스트() {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("password");

        TokenDto token = authService.login(dto);

        TokenDto newToken = authService.reIssueAccessToken(token.getRefreshToken());

        assertThat(newToken.getAccessToken()).isNotNull();
    }

    @Test
    void 토큰_재발급_실패_테스트() {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("password");

        TokenDto token = authService.login(dto);

        redisTemplate.delete(dto.getEmail());

        assertThatThrownBy(() -> authService.reIssueAccessToken(token.getRefreshToken()))
                .isInstanceOf(CustomAuthException.class);
    }

    @Test
    void SNS_로그인_테스트() {
        User user = User.builder()
                .name("tester")
                .email("test@naver.com")
                .password(passwordEncoder.encode("password"))
                .phoneNumber("01012341235")
                .role(Role.USER)
                .provider(Provider.NAVER)
                .build();

        userRepository.save(user);

        TokenDto tokenDto = authService.oAuth2Login(user.getEmail(), user.getRole());

        assertThat(tokenDto.getAccessToken()).isNotBlank();
        assertThat(tokenDto.getRefreshToken()).isNotBlank();
    }
}
