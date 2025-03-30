package com.taxi.userservice.controller;

import com.taxi.common.core.response.CustomResponse;
import com.taxi.common.core.response.ResponseCode;
import com.taxi.userservice.dto.*;
import com.taxi.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    // 일반 회원가입
    @PostMapping("/register")
    public ResponseEntity<CustomResponse<?>> registerUser(@Valid @RequestBody UserRegisterDto dto) {
        userService.registerUser(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.CREATED));
    }

    // SNS 회원가입
    @PostMapping("/oauth/register")
    public ResponseEntity<CustomResponse<?>> registerOAuthUser(@Valid @RequestBody OAuthRegisterDto dto) {
        userService.registerOAuthUser(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.CREATED));
    }

    // 휴대폰 번호 수정
    @PatchMapping("/phone")
    public ResponseEntity<CustomResponse<?>> updatePhoneNumber(@Valid @RequestBody UserPhoneNumberUpdateDto dto) {
        userService.updatePhoneNumber(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

    // 비밀번호 확인
    @PostMapping("/password")
    public ResponseEntity<CustomResponse<UserPasswordValidResponseDto>> isValidPassword(@Valid @RequestBody UserPasswordValidRequestDto dto) {
        UserPasswordValidResponseDto validPassword = userService.isValidPassword(dto);

        return ResponseEntity.ok(CustomResponse.success(validPassword, ResponseCode.SUCCESS));
    }


    // 비밀번호 수정
    @PatchMapping("/password")
    public ResponseEntity<CustomResponse<?>> updatePassword(@Valid @RequestBody UserPasswordUpdateDto dto) {
        userService.updatePassword(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

}
