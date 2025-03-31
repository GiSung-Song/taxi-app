package com.taxi.userservice.controller;

import com.taxi.common.core.dto.UserDto;
import com.taxi.common.core.exception.CustomAuthException;
import com.taxi.common.core.response.CustomResponse;
import com.taxi.common.core.response.ResponseCode;
import com.taxi.userservice.dto.LoginRequestDto;
import com.taxi.userservice.dto.OAuthResponseDto;
import com.taxi.userservice.dto.TokenDto;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.oauth.service.CustomOAuth2User;
import com.taxi.userservice.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/success")
    public ResponseEntity<CustomResponse<?>> oauth2Success(@AuthenticationPrincipal CustomOAuth2User oAuth2User) {
        if (oAuth2User == null) {
            return ResponseEntity.status(ResponseCode.UNAUTHORIZED.getStatus())
                    .body(CustomResponse.error(ResponseCode.UNAUTHORIZED));
        }

        String email = oAuth2User.getEmail();
        User user = authService.getUserByEmail(email);
        Role role = oAuth2User.getRole();

        // 회원가입이 안되어있는 회원이라면 회원가입을 위한 설정
        if (user == null) {
            OAuthResponseDto dto = new OAuthResponseDto(email, oAuth2User.getProvider().name(), oAuth2User.getProviderId(), role.name(), oAuth2User.getName());
            return ResponseEntity.ok(CustomResponse.success(dto, ResponseCode.SUCCESS));
        }

        // 회원가입이 되어있다면 로그인 처리 (토큰 발급)
        TokenDto tokenDto = authService.oAuth2Login(email, role);

        return ResponseEntity.ok(CustomResponse.success(tokenDto, ResponseCode.SUCCESS));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<CustomResponse<?>> login(@RequestBody @Valid LoginRequestDto dto, HttpServletResponse response) {
        TokenDto tokenDto = authService.login(dto);

        Cookie cookie = new Cookie("refreshToken", tokenDto.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 14);

        response.addCookie(cookie);

        return ResponseEntity.ok(CustomResponse.success(tokenDto, ResponseCode.SUCCESS));
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<CustomResponse<?>> logout(@RequestHeader("Authorization") String accessToken) {
        if (accessToken == null || !accessToken.startsWith("Bearer ")) {
            throw new CustomAuthException("유효하지 않은 토큰입니다.");
        }

        accessToken = accessToken.substring(7);

        authService.logout(accessToken);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<CustomResponse<?>> reIssueAccessToken(@CookieValue("refreshToken") String refreshToken) {
        TokenDto tokenDto = authService.reIssueAccessToken(refreshToken);

        return ResponseEntity.ok(CustomResponse.success(tokenDto, ResponseCode.SUCCESS));
    }

    // feign client user 정보 반환
    @GetMapping("/email/{email}")
    public UserDto findUserByEmail(@PathVariable("email") String email) {
        User user = authService.getUserByEmail(email);

        return new UserDto(user.getId(), user.getName(), user.getPhoneNumber());
    }

    // feign client user 정보 반환
    @GetMapping("/id/{id}")
    public UserDto findUserById(@PathVariable("id") Long id) {
        User user = authService.getUserById(id);

        return new UserDto(user.getId(), user.getName(), user.getPhoneNumber());
    }
}