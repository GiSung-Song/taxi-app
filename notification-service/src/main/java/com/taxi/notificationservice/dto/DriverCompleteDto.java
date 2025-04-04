package com.taxi.notificationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DriverCompleteDto {

    private String passengerPhoneNumber; // 승객 전화번호
    private Integer fare;                // 운행 요금
    private String startLocation;        // 출발지
    private String endLocation;          // 목적지
    private LocalDateTime completeTime;  // 운행 완료 시간
}
