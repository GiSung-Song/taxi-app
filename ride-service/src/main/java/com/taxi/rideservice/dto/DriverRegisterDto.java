package com.taxi.rideservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverRegisterDto {

    @NotNull(message = "user ID는 필수 입력 값 입니다.")
    private Long userId;

    @NotBlank(message = "차 번호는 필수 입력 값 입니다.")
    @Length(max = 10, message = "차 번호는 최대 10자 입니다.")
    private String carNumber;

    @Min(value = 1, message = "최대 인원 수는 최소 1명입니다.")
    private int capacity;

    @NotBlank(message = "차 종류는 필수 입력 값 입니다.")
    @Length(max = 50, message = "차 종류는 최대 50자 입니다.")
    private String carName;

    @NotBlank(message = "라이센스 번호는 필수 입력 값 입니다.")
    @Length(max = 30, message = "라이센스 번호는 최대 30자 입니다.")
    private String license;

    @NotBlank(message = "운전자 전화번호는 필수 입력 값 입니다.")
    @Length(max = 20, message = "전화번호는 최대 20자 입니다.")
    private String phoneNumber;

}
