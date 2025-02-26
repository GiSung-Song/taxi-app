package com.taxi.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.security.JwtTokenUtil;
import com.taxi.userservice.config.TestContainerConfig;
import com.taxi.userservice.dto.*;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

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
    void 일반_회원가입_테스트() throws Exception {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setEmail("test2@test.com");
        dto.setName("tester2");
        dto.setRole(Role.USER.name());
        dto.setPassword("password");
        dto.setPhoneNumber("01012341235");

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()));

    }

    @Test
    void SNS_회원가입_테스트() throws Exception {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setProviderId("KakaoProvider");
        dto.setProvider(Provider.KAKAO.name());
        dto.setName("kakao");
        dto.setRole(Role.DRIVER.name());
        dto.setPhoneNumber("01012341236");
        dto.setEmail("kakaoProvider@kakao.com");

        mockMvc.perform(post("/api/user/oauth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(HttpStatus.CREATED.value()));
    }

    @Test
    void 일반_회원가입_실패_테스트_중복_핸드폰번호() throws Exception {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setEmail("test2@test.com");
        dto.setName("tester2");
        dto.setRole(Role.USER.name());
        dto.setPassword("password");
        dto.setPhoneNumber("01012341234");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 일반_회원가입_실패_테스트_입력값_오류() throws Exception {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setEmail("test2@test.com");
        dto.setName("tester2");
        dto.setRole("USERR");
        dto.setPassword("password");
        dto.setPhoneNumber("01012341239");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void SNS_회원가입_실패_테스트_이메일_중복_오류() throws Exception {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setProviderId("KakaoProvider");
        dto.setProvider(Provider.KAKAO.name());
        dto.setName("kakao");
        dto.setRole(Role.DRIVER.name());
        dto.setPhoneNumber("01012341236");
        dto.setEmail("test@test.com");

        mockMvc.perform(post("/api/user/oauth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void SNS_회원가입_실패_테스트_입력값_오류() throws Exception {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setProvider(Provider.KAKAO.name());
        dto.setName("kakao");
        dto.setRole(Role.DRIVER.name());
        dto.setPhoneNumber("01012341236");
        dto.setEmail("kakao@kakao.com");

        mockMvc.perform(post("/api/user/oauth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 휴대폰번호_수정_테스트() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test@test.com", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenUtil.generateAccessToken("test@test.com", Role.USER.name());

        UserPhoneNumberUpdateDto dto = new UserPhoneNumberUpdateDto("01011111111");

        mockMvc.perform(patch("/api/user/phone")
                        .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void 비밀번호_확인_테스트() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test@test.com", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenUtil.generateAccessToken("test@test.com", Role.USER.name());

        UserPasswordValidRequestDto dto = new UserPasswordValidRequestDto("password");

        mockMvc.perform(post("/api/user/password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true));
    }

    @Test
    void 비밀번호_수정_테스트() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "test@test.com", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String accessToken = jwtTokenUtil.generateAccessToken("test@test.com", Role.USER.name());

        UserPasswordUpdateDto dto = new UserPasswordUpdateDto("newPassword");

        mockMvc.perform(patch("/api/user/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
