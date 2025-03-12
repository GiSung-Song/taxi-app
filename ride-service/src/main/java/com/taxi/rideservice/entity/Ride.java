package com.taxi.rideservice.entity;

import com.taxi.rideservice.enums.RideStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long passengerId; //승객 USER ID

    @Column(nullable = false)
    private Long driverId; //기사 DRIVER ID

    @Column(nullable = false)
    private Integer fare; // 요금

    @Column(nullable = false)
    private Double startLatitude; // 시작 위도

    @Column(nullable = false)
    private Double startLongitude; // 시작 경도

    @Column(nullable = false)
    private String startLocation; // 시작 위치

    @Column(nullable = false)
    private Double endLatitude; // 종료 위도

    @Column(nullable = false)
    private Double endLongitude; // 종료 경도

    @Column(nullable = false)
    private String endLocation; // 종료 위치

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RideStatus rideStatus; // 운행 상태

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void completeRide(Integer fare) {
        this.fare = fare;
        this.rideStatus = RideStatus.COMPLETE;
    }

    public void updateRideStatus(RideStatus rideStatus) {
        this.rideStatus = rideStatus;
    }
}
