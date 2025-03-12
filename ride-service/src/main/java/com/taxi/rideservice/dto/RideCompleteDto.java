package com.taxi.rideservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideCompleteDto {

    @NotNull(message = "요금을 입력해주세요.")
    private Integer fare; //요금

    @NotNull(message = "운행번호는 필수 입력 값 입니다.")
    private Long rideId;
}
