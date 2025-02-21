package com.taxi.userservice.dto;

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
public class UserPhoneNumberUpdateDto {

    @NotBlank(message = "휴대전화는 필수 입력값 입니다.")
    @Length(max = 20, message = "최대 20자리 입니다.")
    private String phoneNumber;

}
