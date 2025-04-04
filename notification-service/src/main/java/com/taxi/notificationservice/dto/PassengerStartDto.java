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
public class PassengerStartDto {

    private String driverName;        // 기사 이름
    private String driverPhoneNumber; // 기사 전화번호
    private String carName;           // 차량종류
    private String carNumber;         // 차량번호
    private String startLocation;     // 출발지
    private String endLocation;       // 목적지
    private LocalDateTime startTime;  // 운행 시작 시간
}
