package com.taxi.userservice.dto;

import com.taxi.common.core.valid.ValidEnum;
import com.taxi.userservice.enums.Provider;
import com.taxi.userservice.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuthRegisterDto {

    @ValidEnum(enumClass = Role.class, message = "정확한 role을 입력해주세요.")
    @NotBlank(message = "role은 필수 입력값 입니다.")
    private String role;

    @ValidEnum(enumClass = Provider.class, message = "정확한 SNS 종류를 입력해주세요.")
    @NotBlank(message = "SNS 종류는 필수 입력값 입니다.")
    private String provider;

    @NotBlank(message = "SNS ID는 필수 입력값 입니다.")
    private String providerId;

    @NotBlank(message = "이메일은 필수 입력값 입니다.")
    @Email(message = "이메일 형식으로 입력해주세요.")
    @Length(max = 50, message = "최대 50자리 입니다.")
    private String email;

    @NotBlank(message = "이름은 필수 입력값 입니다.")
    @Length(max = 30, message = "최대 30자리 입니다.")
    private String name;

    @NotBlank(message = "휴대전화는 필수 입력값 입니다.")
    @Length(max = 20, message = "최대 20자리 입니다.")
    private String phoneNumber;


}
