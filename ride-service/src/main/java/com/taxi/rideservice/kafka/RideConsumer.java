package com.taxi.rideservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.exception.CustomInternalException;
import com.taxi.rideservice.dto.RideCallRequestDto;
import com.taxi.rideservice.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideConsumer {

    private final RideService rideService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ride-request")
    public void consumeRideRequest(String message) {
        try {
            log.info("Received Message : {}", message);
            RideCallRequestDto dto = objectMapper.readValue(message, RideCallRequestDto.class);

            rideService.saveCallRequest(dto);
        } catch (Exception e) {
            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }
    }

}
