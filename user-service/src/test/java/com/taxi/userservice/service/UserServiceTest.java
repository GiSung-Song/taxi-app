package com.taxi.userservice.service;

import com.taxi.common.exception.CustomBadRequestException;
import com.taxi.userservice.dto.*;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("h2")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void 일반_회원가입_테스트() {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setName("테스터");
        dto.setPassword("password");
        dto.setEmail("test@email.com");
        dto.setRole(Role.USER.name());
        dto.setPhoneNumber("01012341234");

        userService.registerUser(dto);

        verify(userRepository, times(1)).save(argThat(user ->
                user.getEmail().equals(dto.getEmail()) &&
                user.getProvider().equals(Provider.LOCAL) &&
                user.getPhoneNumber().equals(dto.getPhoneNumber()) &&
                user.getRole().equals(Role.USER)));
    }

    @Test
    void 일반_회원가입_실패_테스트() {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setName("테스터");
        dto.setPassword("password");
        dto.setEmail("test@email.com");
        dto.setRole(Role.USER.name());
        dto.setPhoneNumber("01012341234");

        User user = User.builder()
                .email("test@email.com")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(user);

        Assertions.assertThrows(CustomBadRequestException.class, () -> userService.registerUser(dto));
    }

    @Test
    void 일반_회원가입_실패_테스트2() {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setName("테스터");
        dto.setPassword("password");
        dto.setEmail("test@email.com");
        dto.setRole(Role.USER.name());
        dto.setPhoneNumber("01012341234");

        User user = User.builder()
                .email("test@email.com")
                .phoneNumber("01012341234")
                .build();

        when(userRepository.findByPhoneNumber(anyString())).thenReturn(user);

        Assertions.assertThrows(CustomBadRequestException.class, () -> userService.registerUser(dto));
    }

    @Test
    void SNS_회원가입_테스트() {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setName("테스터");
        dto.setEmail("43217984372189@kakao.com");
        dto.setRole(Role.DRIVER.name());
        dto.setPhoneNumber("01012341234");
        dto.setProviderId("43217984372189");
        dto.setProvider("KAKAO");

        userService.registerOAuthUser(dto);

        verify(userRepository, times(1)).save(argThat(user ->
                user.getEmail().equals(dto.getEmail()) &&
                user.getProviderId().equals(dto.getProviderId()) &&
                user.getProvider().name().equals(dto.getProvider())));
    }

    @Test
    void SNS_회원가입_실패_테스트() {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setName("테스터");
        dto.setEmail("43217984372189@kakao.com");
        dto.setRole(Role.DRIVER.name());
        dto.setPhoneNumber("01012341234");
        dto.setProviderId("43217984372189");
        dto.setProvider("KAKAO");

        User user = User.builder()
                .email("43217984372189@kakao.com")
                .phoneNumber("01012341233")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(user);

        Assertions.assertThrows(CustomBadRequestException.class, () -> userService.registerOAuthUser(dto));
    }

    @Test
    void SNS_회원가입_실패_테스트2() {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setName("테스터");
        dto.setEmail("43217984372189@kakao.com");
        dto.setRole(Role.DRIVER.name());
        dto.setPhoneNumber("01012341234");
        dto.setProviderId("43217984372189");
        dto.setProvider("KAKAO");

        User user = User.builder()
                .email("43217984372189@naver.com")
                .phoneNumber("01012341234")
                .build();

        when(userRepository.findByPhoneNumber(anyString())).thenReturn(user);

        Assertions.assertThrows(CustomBadRequestException.class, () -> userService.registerOAuthUser(dto));
    }

    @Test
    void 회원_휴대폰번호_변경_테스트() {
        User user = User.builder()
                .email("43217984372189@naver.com")
                .phoneNumber("01012341234")
                .role(Role.USER)
                .build();

        setUpLoginUser(user);

        UserPhoneNumberUpdateDto dto = new UserPhoneNumberUpdateDto("01012341231");

        userService.updatePhoneNumber(dto);

        assertEquals(dto.getPhoneNumber(), user.getPhoneNumber());
    }

    @Test
    void 회원_비밀번호_확인_테스트() {
        when(passwordEncoder.encode("12341234")).thenReturn("encoded12341234");

        User user = User.builder()
                .password(passwordEncoder.encode("12341234"))
                .email("43217984372189@naver.com")
                .phoneNumber("01012341234")
                .role(Role.USER)
                .build();

        setUpLoginUser(user);

        UserPasswordValidRequestDto dtoTrue = new UserPasswordValidRequestDto("12341234");
        UserPasswordValidRequestDto dtoFalse = new UserPasswordValidRequestDto("12341233");

        when(passwordEncoder.matches("12341234", "encoded12341234")).thenReturn(true);
        when(passwordEncoder.matches("12341233", "encoded12341234")).thenReturn(false);

        UserPasswordValidResponseDto dtoT = userService.isValidPassword(dtoTrue);
        UserPasswordValidResponseDto dtoF = userService.isValidPassword(dtoFalse);

        assertEquals(true, dtoT.isValid());
        assertEquals(false, dtoF.isValid());
    }

    @Test
    void 회원_비밀번호_수정_테스트() {
        when(passwordEncoder.encode("12341234")).thenReturn("encoded12341234");

        User user = User.builder()
                .password(passwordEncoder.encode("12341234"))
                .email("43217984372189@naver.com")
                .phoneNumber("01012341234")
                .role(Role.USER)
                .build();

        setUpLoginUser(user);

        UserPasswordUpdateDto dto = new UserPasswordUpdateDto("newpassword");

        when(passwordEncoder.encode("newpassword")).thenReturn("encodednewpassword");

        userService.updatePassword(dto);

        assertEquals("encodednewpassword", user.getPassword());
    }

    void setUpLoginUser(User user) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(userRepository.findByEmail(user.getEmail())).thenReturn(user);

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}