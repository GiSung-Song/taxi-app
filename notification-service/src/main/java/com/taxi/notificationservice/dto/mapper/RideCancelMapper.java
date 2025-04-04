package com.taxi.notificationservice.dto.mapper;

import com.taxi.common.core.dto.RideCancelDto;
import com.taxi.notificationservice.dto.DriverCancelDto;
import com.taxi.notificationservice.dto.PassengerCancelDto;

public class RideCancelMapper {
    public static PassengerCancelDto toPassengerCancelDto(RideCancelDto rideCancelDto) {
        PassengerCancelDto passengerCancelDto = new PassengerCancelDto();

        passengerCancelDto.setCancelTime(rideCancelDto.getCancelTime());

        return passengerCancelDto;
    }

    public static DriverCancelDto toDriverCancelDto(RideCancelDto rideCancelDto) {
        DriverCancelDto driverCancelDto = new DriverCancelDto();

        driverCancelDto.setCancelTime(rideCancelDto.getCancelTime());

        return driverCancelDto;
    }
}
