package com.taxi.rideservice.repository;

import com.taxi.rideservice.entity.Ride;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RideRepository extends JpaRepository<Ride, Long> {
}
