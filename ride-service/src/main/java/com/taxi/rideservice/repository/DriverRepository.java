package com.taxi.rideservice.repository;

import com.taxi.rideservice.entity.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Driver findByUserId(Long userId);

}