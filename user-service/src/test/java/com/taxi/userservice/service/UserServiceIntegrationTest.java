package com.taxi.userservice.service;

import com.taxi.common.exception.CustomBadRequestException;
import com.taxi.userservice.config.TestContainerConfig;
import com.taxi.userservice.dto.*;
import com.taxi.userservice.entity.User;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import com.taxi.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String testEmail = "test@email.com";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .name("tester")
                .email(testEmail)
                .password(passwordEncoder.encode("password"))
                .phoneNumber("01012341234")
                .role(Role.USER)
                .provider(Provider.LOCAL)
                .build();

        userRepository.save(user);

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                testEmail, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void 일반_회원가입_테스트() {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setName("tester2");
        dto.setEmail("testtttttt@email.com");
        dto.setRole(Role.USER.name());
        dto.setPassword("password");
        dto.setPhoneNumber("01012341235");

        userService.registerUser(dto);

        User user = userRepository.findByEmail(dto.getEmail());

        assertThat(user).isNotNull();
        assertThat(user.getProvider()).isEqualTo(Provider.LOCAL);
        assertThat(user.getPhoneNumber()).isEqualTo(dto.getPhoneNumber());
    }

    @Test
    void SNS_회원가입_테스트() {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setName("tester3");
        dto.setEmail("123809123@kakao.com");
        dto.setProvider("123809123");
        dto.setProvider(Provider.KAKAO.name());
        dto.setRole(Role.DRIVER.name());
        dto.setPhoneNumber("01012341236");

        userService.registerOAuthUser(dto);

        User user = userRepository.findByEmail(dto.getEmail());

        assertThat(user).isNotNull();
        assertThat(user.getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(user.getEmail()).isEqualTo(dto.getEmail());
    }

    @Test
    void 일반_회원가입_실패_테스트() {
        UserRegisterDto dto = new UserRegisterDto();

        dto.setName("tester");
        dto.setEmail(testEmail);
        dto.setRole(Role.USER.name());
        dto.setPassword("password");
        dto.setPhoneNumber("01012341235");

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(CustomBadRequestException.class);

        dto.setPhoneNumber("01012341234");
        dto.setEmail("test@test.com");

        assertThatThrownBy(() -> userService.registerUser(dto))
                .isInstanceOf(CustomBadRequestException.class);
    }

    @Test
    void SNS_회원가입_실패_테스트() {
        OAuthRegisterDto dto = new OAuthRegisterDto();

        dto.setName("tester");
        dto.setEmail(testEmail);
        dto.setRole(Role.USER.name());
        dto.setProvider(Provider.NAVER.name());
        dto.setProviderId("9108723947089715082");
        dto.setPhoneNumber("01012341235");

        assertThatThrownBy(() -> userService.registerOAuthUser(dto))
                .isInstanceOf(CustomBadRequestException.class);

        dto.setEmail("test@naver.com");
        dto.setPhoneNumber("01012341234");

        assertThatThrownBy(() -> userService.registerOAuthUser(dto))
                .isInstanceOf(CustomBadRequestException.class);
    }

    @Test
    void 휴대폰번호_수정_테스트() {
        UserPhoneNumberUpdateDto dto = new UserPhoneNumberUpdateDto("01012341239");

        userService.updatePhoneNumber(dto);

        User updateUser = userRepository.findByEmail(testEmail);

        assertThat(updateUser.getPhoneNumber()).isEqualTo(dto.getPhoneNumber());
    }

    @Test
    void 회원_비밀번호_확인_테스트() {
        UserPasswordValidRequestDto dto = new UserPasswordValidRequestDto("password");
        UserPasswordValidResponseDto validPassword = userService.isValidPassword(dto);

        UserPasswordValidRequestDto dto2 = new UserPasswordValidRequestDto("passwordddddddd");
        UserPasswordValidResponseDto validPassword2 = userService.isValidPassword(dto2);

        assertThat(validPassword.isValid()).isEqualTo(true);
        assertThat(validPassword2.isValid()).isEqualTo(false);
    }

    @Test
    void 회원_비밀번호_수정_테스트() {
        UserPasswordUpdateDto dto = new UserPasswordUpdateDto("newPassword");

        userService.updatePassword(dto);

        User updateUser = userRepository.findByEmail(testEmail);

        assertThat(passwordEncoder.matches(dto.getPassword(), updateUser.getPassword())).isTrue();
    }

}