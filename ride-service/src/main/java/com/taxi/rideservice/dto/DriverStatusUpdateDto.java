package com.taxi.rideservice.dto;

import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.common.valid.ValidEnum;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatusUpdateDto {

    @NotBlank(message = "이메일은 필수 입력 값 입니다.")
    private String email;

    @ValidEnum(enumClass = DriverStatus.class)
    @NotBlank(message = "상태는 필수 입력 값 입니다.")
    private String driverStatus;

}
