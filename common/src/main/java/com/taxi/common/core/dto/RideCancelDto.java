package com.taxi.common.core.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RideCancelDto {

    private Long rideId;
    private Long driverUserId;
    private LocalDateTime cancelTime; // 호출 취소 시간
    private String rideStatus;        // 운행 상태

}
