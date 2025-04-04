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
public class DriverAcceptDto {

    private String passengerPhoneNumber; // 승객 전화번호
    private String startLocation;        // 출발지
    private String endLocation;          // 목적지
    private LocalDateTime acceptTime;    // 호출 수락 시간
}