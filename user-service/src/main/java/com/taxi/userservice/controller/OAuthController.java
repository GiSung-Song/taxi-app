package com.taxi.userservice.controller;

import com.taxi.common.response.CustomResponse;
import com.taxi.common.response.ResponseCode;
import com.taxi.userservice.dto.OAuthResponseDto;
import com.taxi.userservice.dto.TokenDto;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.oauth.service.CustomOAuth2User;
import com.taxi.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class OAuthController {

    private final AuthService authService;

    @GetMapping("/success")
    public ResponseEntity<CustomResponse<?>> oauth2Success(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomOAuth2User)) {
            return ResponseEntity.status(ResponseCode.UNAUTHORIZED.getStatus())
                    .body(CustomResponse.error(ResponseCode.UNAUTHORIZED));
        }

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getEmail();
        User user = authService.getUser(email);
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

}
