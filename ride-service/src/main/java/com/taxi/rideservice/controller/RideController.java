package com.taxi.rideservice.controller;

import com.taxi.common.core.dto.DriveCompleteDto;
import com.taxi.common.core.dto.RideAcceptDto;
import com.taxi.common.core.dto.RideCancelDto;
import com.taxi.common.core.dto.RideStartDto;
import com.taxi.common.core.response.CustomResponse;
import com.taxi.common.core.response.ResponseCode;
import com.taxi.rideservice.dto.*;
import com.taxi.rideservice.kafka.RideProducer;
import com.taxi.rideservice.service.RideService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ride")
@RequiredArgsConstructor
public class RideController {

    private final RideService rideService;
    private final RideProducer rideProducer;

    @PostMapping("/call")
    public ResponseEntity<CustomResponse<?>> callTaxi(@RequestBody @Valid RideCallRequestDto dto) {
        rideProducer.sendRideRequest(dto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

    @PostMapping("/find")
    public ResponseEntity<CustomResponse<List<CallResponseDto>>> findNearbyCall(@RequestBody @Valid FindCallRequestDto dto) {
        List<CallResponseDto> nearbyCall = rideService.findNearbyCall(dto);

        return ResponseEntity.ok(CustomResponse.success(nearbyCall, ResponseCode.SUCCESS));
    }

    @PostMapping("/accept")
    public ResponseEntity<CustomResponse<?>> acceptCall(@RequestBody @Valid CallAcceptRequestDto dto) {
        RideAcceptDto rideInfoDto = rideService.acceptCall(dto);
        rideProducer.sendRideAccept(rideInfoDto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

    @PostMapping("/cancel/{rideId}")
    public ResponseEntity<CustomResponse<?>> cancelCall(@PathVariable Long rideId) {
        RideCancelDto rideCancelDto = rideService.cancelRide(rideId);
        rideProducer.sendRideCancel(rideCancelDto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

    @PostMapping("/start/{rideId}")
    public ResponseEntity<CustomResponse<?>> startRide(@PathVariable Long rideId) {
        RideStartDto rideStartDto = rideService.startRide(rideId);
        rideProducer.sendRideStart(rideStartDto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

    @PostMapping("/complete")
    public ResponseEntity<CustomResponse<?>> completeRide(@RequestBody @Valid RideCompleteDto dto) {
        DriveCompleteDto driveCompleteDto = rideService.completeRide(dto);
        rideProducer.sendRideComplete(driveCompleteDto);

        return ResponseEntity.ok(CustomResponse.success(null, ResponseCode.SUCCESS));
    }

}
