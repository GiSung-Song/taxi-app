package com.taxi.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.security.JwtTokenFilter;
import com.taxi.userservice.dto.LoginRequestDto;
import com.taxi.userservice.dto.TokenDto;
import com.taxi.userservice.oauth.service.CustomOAuth2User;
import com.taxi.userservice.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(value = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtTokenFilter.class))
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private CustomOAuth2User customOAuth2User;

    @Test
    void 로그인_테스트() throws Exception {
        LoginRequestDto dto = new LoginRequestDto();

        dto.setEmail("email@email.com");
        dto.setPassword("password");

        TokenDto tokenDto = new TokenDto("AccessToken", "RefreshToken");

        when(authService.login(any())).thenReturn(tokenDto);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.data.accessToken").value("AccessToken"));
    }

    @Test
    void 로그아웃_테스트() throws Exception {
        String authorizationHeader = "Bearer accessToken";

        doNothing().when(authService).logout(anyString());

        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    void 토큰_재발급_테스트() throws Exception {
        String refreshToken = "refreshToken";
        TokenDto tokenDto = new TokenDto();
        tokenDto.setAccessToken("accessToken");

        when(authService.reIssueAccessToken(refreshToken)).thenReturn(tokenDto);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andDo(print());
    }

}
