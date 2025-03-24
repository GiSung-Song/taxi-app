package com.taxi.rideservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideInfoDto {

    private Long rideId;

    // 기사가 확인할 정보
    private Long passengerUserId;         // 승객 User ID
    private String passengerName;         // 승객 이름
    private String passengerPhoneNumber;  // 승객 전화번호
    private String startLocation;         // 출발지
    private String endLocation;           // 목적지

    // 승객이 확인할 정보
    private Long driverUserId;        // 기사 User ID
    private String driverName;        // 기사 이름
    private String driverPhoneNumber; // 기사 전화번호
    private String carName;           // 차량종류
    private String carNumber;         // 차량번호
    private int capacity;             // 수용인원
    private Integer totalRides;       // 운행횟수

}
