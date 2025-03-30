package com.taxi.rideservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FindCallRequestDto {

    @DecimalMin(value = "-90.0", message = "위도는 최소 -90도 입니다.")
    @DecimalMax(value = "90.0", message = "위도는 최대 90도 입니다.")
    @NotNull(message = "위도는 필수 입력 값 입니다.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도는 최소 -180도 입니다.")
    @DecimalMax(value = "180.0", message = "경도는 최대 180도 입니다.")
    @NotNull(message = "경도는 필수 입력 값 입니다.")
    private Double longitude;
}