package com.taxi.notificationservice.dto.mapper;

import com.taxi.common.core.dto.DriveCompleteDto;
import com.taxi.notificationservice.dto.DriverCompleteDto;
import com.taxi.notificationservice.dto.PassengerCompleteDto;

public class RideCompleteMapper {
    public static PassengerCompleteDto toPassengerCompleteDto(DriveCompleteDto dto) {
        PassengerCompleteDto passengerCompleteDto = new PassengerCompleteDto();

        passengerCompleteDto.setDriverName(dto.getDriverName());
        passengerCompleteDto.setDriverPhoneNumber(dto.getDriverPhoneNumber());
        passengerCompleteDto.setCarName(dto.getCarName());
        passengerCompleteDto.setCarNumber(dto.getCarNumber());
        passengerCompleteDto.setFare(dto.getFare());
        passengerCompleteDto.setStartLocation(dto.getStartLocation());
        passengerCompleteDto.setEndLocation(dto.getEndLocation());
        passengerCompleteDto.setCompleteTime(dto.getCompleteTime());

        return passengerCompleteDto;
    }

    public static DriverCompleteDto toDriverCompleteDto(DriveCompleteDto dto) {
        DriverCompleteDto driverCompleteDto = new DriverCompleteDto();

        driverCompleteDto.setPassengerPhoneNumber(dto.getPassengerPhoneNumber());
        driverCompleteDto.setStartLocation(dto.getStartLocation());
        driverCompleteDto.setEndLocation(dto.getEndLocation());
        driverCompleteDto.setFare(dto.getFare());
        driverCompleteDto.setCompleteTime(dto.getCompleteTime());

        return driverCompleteDto;
    }
}
