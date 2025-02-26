package com.taxi.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.security.JwtTokenUtil;
import com.taxi.userservice.config.TestContainerConfig;
import com.taxi.userservice.dto.LoginRequestDto;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class AutoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void 로그인_테스트() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        String refreshToken = redisTemplate.opsForValue().get("test@test.com");

        Assertions.assertNotNull(refreshToken);
    }

    @Test
    void 로그인_비밀번호_실패_테스트() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@test.com");
        dto.setPassword("passwordd");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그인_이메일_없음_테스트() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("testt@test.com");
        dto.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그인_잘못된_로그인방식_테스트() throws Exception {
        User user = User.builder()
                .name("tester")
                .email("test@naver.com")
                .phoneNumber("01012341237")
                .role(Role.USER)
                .provider(Provider.NAVER)
                .providerId("2134214321")
                .build();

        userRepository.save(user);

        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("test@naver.com");
        dto.setPassword("password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 로그아웃_테스트() throws Exception {
        String accessToken = jwtTokenUtil.generateAccessToken("test@test.com", Role.USER.name());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andDo(print());

        String hashToken = jwtTokenUtil.tokenToHash(accessToken);

        String isLogout = redisTemplate.opsForValue().get(hashToken);

        Assertions.assertEquals("logout", isLogout);
    }

    @Test
    void 로그아웃_토큰오류_테스트() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + "fdsafdsafewqfdsa.fdsafdsaf.dsafdsa"))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    void 토큰_재발급_테스트() throws Exception {
        String refreshToken = jwtTokenUtil.generateRefreshToken("test@test.com");
        redisTemplate.opsForValue().set("test@test.com", refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andDo(print());
    }

    @Test
    void 토큰_재발급_토큰오류_테스트() throws Exception {
        String refreshToken = jwtTokenUtil.generateRefreshToken("test@test.com");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }
}
