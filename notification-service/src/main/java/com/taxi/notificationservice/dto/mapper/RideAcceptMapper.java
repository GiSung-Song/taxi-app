package com.taxi.notificationservice.dto.mapper;

import com.taxi.common.core.dto.RideAcceptDto;
import com.taxi.notificationservice.dto.DriverAcceptDto;
import com.taxi.notificationservice.dto.PassengerAcceptDto;

public class RideAcceptMapper {
    public static PassengerAcceptDto toPassengerAcceptDto(RideAcceptDto rideAcceptDto) {
        PassengerAcceptDto passengerAcceptDto = new PassengerAcceptDto();

        passengerAcceptDto.setDriverName(rideAcceptDto.getDriverName());
        passengerAcceptDto.setDriverPhoneNumber(rideAcceptDto.getDriverPhoneNumber());
        passengerAcceptDto.setCarName(rideAcceptDto.getCarName());
        passengerAcceptDto.setCarNumber(rideAcceptDto.getCarNumber());
        passengerAcceptDto.setCapacity(rideAcceptDto.getCapacity());
        passengerAcceptDto.setTotalRides(rideAcceptDto.getTotalRides());
        passengerAcceptDto.setStartLocation(rideAcceptDto.getStartLocation());
        passengerAcceptDto.setEndLocation(rideAcceptDto.getEndLocation());
        passengerAcceptDto.setAcceptTime(rideAcceptDto.getAcceptTime());

        return passengerAcceptDto;
    }

    public static DriverAcceptDto toDriverAcceptDto(RideAcceptDto rideAcceptDto) {
        DriverAcceptDto driverAcceptDto = new DriverAcceptDto();

        driverAcceptDto.setPassengerPhoneNumber(rideAcceptDto.getPassengerPhoneNumber());
        driverAcceptDto.setStartLocation(rideAcceptDto.getStartLocation());
        driverAcceptDto.setEndLocation(rideAcceptDto.getEndLocation());
        driverAcceptDto.setAcceptTime(rideAcceptDto.getAcceptTime());

        return driverAcceptDto;
    }
}
