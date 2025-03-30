package com.taxi.userservice.service;

import com.taxi.common.core.exception.CustomAuthException;
import com.taxi.common.core.exception.CustomBadRequestException;
import com.taxi.userservice.dto.*;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 일반 회원가입
    @Transactional
    public void registerUser(UserRegisterDto dto) {
        isValidRegister(dto.getEmail(), dto.getPhoneNumber());

        User user = User.builder()
                .name(dto.getName())
                .password(passwordEncoder.encode(dto.getPassword()))
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .role(Role.valueOf(dto.getRole()))
                .provider(Provider.LOCAL)
                .build();

        userRepository.save(user);
    }

    // SNS 회원가입
    @Transactional
    public void registerOAuthUser(OAuthRegisterDto dto) {
        isValidRegister(dto.getEmail(), dto.getPhoneNumber());

        User user = User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .phoneNumber(dto.getPhoneNumber())
                .role(Role.valueOf(dto.getRole()))
                .provider(Provider.valueOf(dto.getProvider()))
                .providerId(dto.getProviderId())
                .build();

        userRepository.save(user);
    }

    private void isValidRegister(String email, String phoneNumber) {
        User user = userRepository.findByEmail(email);

        if (user != null) {
            log.error("이미 회원가입 된 이메일입니다.");
            throw new CustomBadRequestException("이미 회원가입 된 이메일 입니다.");
        }

        user = userRepository.findByPhoneNumber(phoneNumber);

        if (user != null) {
            log.error("이미 회원가입 된 휴대폰 번호입니다.");
            throw new CustomBadRequestException("이미 회원가입 된 휴대폰 번호입니다.");
        }
    }

    // 회원정보(휴대폰 번호) 수정
    @Transactional
    public void updatePhoneNumber(UserPhoneNumberUpdateDto dto) {
        User user = getLoggedInUser();

        user.updatePhoneNumber(dto.getPhoneNumber());
    }

    // 회원 비밀번호 확인
    @Transactional(readOnly = true)
    public UserPasswordValidResponseDto isValidPassword(UserPasswordValidRequestDto dto) {
        User user = getLoggedInUser();

        if (user.getProviderId() != null) {
            throw new CustomBadRequestException("SNS 회원은 비밀번호가 없습니다.");
        }

        UserPasswordValidResponseDto validResponseDto = new UserPasswordValidResponseDto();

        if (passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            validResponseDto.setValid(true);
        } else {
            validResponseDto.setValid(false);
        }

        return validResponseDto;
    }

    // 비밀번호 수정
    @Transactional
    public void updatePassword(UserPasswordUpdateDto dto) {
        User user = getLoggedInUser();

        if (user.getProviderId() != null) {
            throw new CustomBadRequestException("SNS 회원은 비밀번호를 입력할 수 없습니다.");
        }

        user.updatePassword(passwordEncoder.encode(dto.getPassword()));
    }

    // 현재 로그인된 사용자 조회
    private User getLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User user = userRepository.findByEmail(email);

        if (user == null) {
            throw new CustomAuthException("로그인 되어있지 않습니다.");
        }

        return user;
    }
}
