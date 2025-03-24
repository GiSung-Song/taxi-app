package com.taxi.rideservice.controller;

import com.taxi.common.response.CustomResponse;
import com.taxi.common.response.ResponseCode;
import com.taxi.rideservice.dto.DriverRegisterDto;
import com.taxi.rideservice.dto.DriverStatusUpdateDto;
import com.taxi.rideservice.dto.DriverUpdateDto;
import com.taxi.rideservice.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final DriverService driverService;

    @PostMapping("/register")
    public ResponseEntity<CustomResponse<?>> registerDriver(@RequestBody @Valid DriverRegisterDto dto) {
        driverService.registerDriver(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.CREATED));
    }

    @PatchMapping("/info")
    public ResponseEntity<CustomResponse<?>> updateDriverInfo(@RequestBody @Valid DriverUpdateDto dto) {
        driverService.updateDriver(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

    @PatchMapping("/status")
    public ResponseEntity<CustomResponse<?>> updateDriverStatus(@RequestBody @Valid DriverStatusUpdateDto dto) {
        driverService.updateDriverStatus(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }
}
