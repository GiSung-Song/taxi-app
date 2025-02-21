package com.taxi.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.security.JwtTokenFilter;
import com.taxi.userservice.dto.*;
import com.taxi.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = UserController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtTokenFilter.class))
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    void 일반_회원가입_테스트() throws Exception {
        UserRegisterDto dto = new UserRegisterDto("test@test.com", "테스터", "rawpassword", "01012341234", "USER");

        mockMvc.perform(post("/api/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void 일반_회원가입_실패_테스트() throws Exception {
        UserRegisterDto dto = new UserRegisterDto("test@test.com", "테스터", "rawpassword", "01012341234", "USERR");

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void SNS_회원가입_테스트() throws Exception {
        OAuthRegisterDto dto = new OAuthRegisterDto("DRIVER", "KAKAO", "fd9s0af9d0",
                "fd9s0af9d0@kakao.com", "드라이버", "01012341231");

        mockMvc.perform(post("/api/user/oauth/register")
                .content(new ObjectMapper().writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void SNS_회원가입_실패_테스트() throws Exception {
        OAuthRegisterDto dto = new OAuthRegisterDto("DRIVER", "FACEBOOK", "fd9s0af9d0",
                "fd9s0af9d0@kakao.com", "드라이버", "01012341231");

        mockMvc.perform(post("/api/user/oauth/register")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void 휴대폰_번호_수정_테스트() throws Exception {
        UserPhoneNumberUpdateDto dto = new UserPhoneNumberUpdateDto("01012345678");

        mockMvc.perform(patch("/api/user/phone")
                .content(new ObjectMapper().writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void 휴대폰_번호_수정_실패_테스트() throws Exception {
        UserPhoneNumberUpdateDto dto = new UserPhoneNumberUpdateDto();

        mockMvc.perform(patch("/api/user/phone")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void 비밀번호_확인_테스트() throws Exception {
        UserPasswordValidRequestDto request = new UserPasswordValidRequestDto("password");
        UserPasswordValidResponseDto response = new UserPasswordValidResponseDto(true);

        when(userService.isValidPassword(any())).thenReturn(response);

        mockMvc.perform(post("/api/user/password")
                        .content(new ObjectMapper().writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid").value(true))
                .andDo(print());
    }

    @Test
    void 비밀번호_확인_실패_테스트() throws Exception {
        UserPasswordValidRequestDto request = new UserPasswordValidRequestDto();
        UserPasswordValidResponseDto response = new UserPasswordValidResponseDto(true);

        when(userService.isValidPassword(any())).thenReturn(response);

        mockMvc.perform(post("/api/user/password")
                        .content(new ObjectMapper().writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void 비밀번호_수정_테스트() throws Exception {
        UserPasswordUpdateDto dto = new UserPasswordUpdateDto("newPassword");

        mockMvc.perform(patch("/api/user/password")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void 비밀번호_수정_실패_테스트() throws Exception {
        UserPasswordUpdateDto dto = new UserPasswordUpdateDto();

        mockMvc.perform(patch("/api/user/password")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }
}