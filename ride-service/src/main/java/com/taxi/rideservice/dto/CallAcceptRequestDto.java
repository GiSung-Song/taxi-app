package com.taxi.rideservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallAcceptRequestDto {

    @NotBlank(message = "승객 이메일은 필수 입력 값 입니다.")
    private String passengerEmail;

    @NotBlank(message = "기사 이메일은 필수 입력 값 입니다.")
    private String driverEmail;
}