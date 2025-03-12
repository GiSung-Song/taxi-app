package com.taxi.rideservice.entity;

import com.taxi.rideservice.enums.DriverStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 10)
    private String carNumber;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false, length = 50)
    private String carName;

    @Column(nullable = false, length = 30)
    private String license;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DriverStatus driverStatus;

    @Column(nullable = false)
    private Integer totalRides;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void updateCarNumber(String carNumber) {
        this.carNumber = carNumber;
    }

    public void updateCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void updateCarName(String carName) {
        this.carName = carName;
    }

    public void updateLicense(String license) {
        this.license = license;
    }

    public void updatePhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void updateDriverStatus(DriverStatus driverStatus) {
        this.driverStatus = driverStatus;
    }

    public void finishRide() {
        this.totalRides += 1;
        this.driverStatus = DriverStatus.WAITING;
    }
}
