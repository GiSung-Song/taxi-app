package com.taxi.rideservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideCallRequestDto {

    @NotBlank(message = "승객 이메일은 필수 입력 값 입니다.")
    private String passengerEmail;

    @DecimalMin(value = "-90.0", message = "위도는 최소 -90도 입니다.")
    @DecimalMax(value = "90.0", message = "위도는 최대 90도 입니다.")
    private Double startLatitude;

    @DecimalMin(value = "-180.0", message = "경도는 최소 -180도 입니다.")
    @DecimalMax(value = "180.0", message = "경도는 최대 180도 입니다.")
    private Double startLongitude;

    @NotBlank(message = "출발지를 정확하게 입력해주세요.")
    private String startLocation;

    @DecimalMin(value = "-90.0", message = "위도는 최소 -90도 입니다.")
    @DecimalMax(value = "90.0", message = "위도는 최대 90도 입니다.")
    private Double endLatitude;

    @DecimalMin(value = "-180.0", message = "경도는 최소 -180도 입니다.")
    @DecimalMax(value = "180.0", message = "경도는 최대 180도 입니다.")
    private Double endLongitude;

    @NotBlank(message = "목적지를 정확하게 입력해주세요.")
    private String endLocation;

}
