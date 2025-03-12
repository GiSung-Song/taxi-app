package com.taxi.rideservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CallResponseDto {

    private String passengerEmail;
    private String startLocation;
    private String endLocation;

}
