package com.taxi.notificationservice.dto.mapper;

import com.taxi.common.core.dto.RideStartDto;
import com.taxi.notificationservice.dto.DriverStartDto;
import com.taxi.notificationservice.dto.PassengerStartDto;

public class RideStartMapper {
    public static PassengerStartDto toPassengerStartDto(RideStartDto rideStartDto) {
        PassengerStartDto passengerStartDto = new PassengerStartDto();

        passengerStartDto.setDriverName(rideStartDto.getDriverName());
        passengerStartDto.setDriverPhoneNumber(rideStartDto.getDriverPhoneNumber());
        passengerStartDto.setCarName(rideStartDto.getCarName());
        passengerStartDto.setCarNumber(rideStartDto.getCarNumber());
        passengerStartDto.setStartLocation(rideStartDto.getStartLocation());
        passengerStartDto.setEndLocation(rideStartDto.getEndLocation());
        passengerStartDto.setStartTime(rideStartDto.getStartTime());

        return passengerStartDto;
    }

    public static DriverStartDto toDriverStartDto(RideStartDto rideStartDto) {
        DriverStartDto driverStartDto = new DriverStartDto();

        driverStartDto.setPassengerPhoneNumber(rideStartDto.getPassengerPhoneNumber());
        driverStartDto.setStartLocation(rideStartDto.getStartLocation());
        driverStartDto.setEndLocation(rideStartDto.getEndLocation());
        driverStartDto.setStartTime(rideStartDto.getStartTime());

        return driverStartDto;
    }
}
