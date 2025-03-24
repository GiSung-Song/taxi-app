package com.taxi.rideservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.exception.CustomInternalException;
import com.taxi.rideservice.dto.RideCallRequestDto;
import com.taxi.rideservice.dto.RideInfoDto;
import com.taxi.rideservice.service.RideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RideService rideService;
    private static final String TOPIC = "ride-request";

    public void sendRideRequest(RideCallRequestDto dto) {
        try {
            String rideRequestJson = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send(TOPIC, rideRequestJson);

            log.info("Success To Send : {}", rideRequestJson);
        } catch (JsonProcessingException e) {
            log.error("데이터 변환 중 내부적인 오류가 발생 : {}", e.getMessage());
            throw new CustomInternalException("데이터 변환 중 내부적인 오류가 발생하였습니다.");
        }
    }

    public void sendRideInfo(RideInfoDto dto) {
        try {
            String rideInfo = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("ride-accept", rideInfo);
        } catch (JsonProcessingException e) {
            log.error("데이터 변환 중 내부적인 오류가 발생 : {}", e.getMessage());

            // 롤백 처리 (ride 삭제 및 운행 상태 변경)
            rideService.rollBackRide(dto);

            throw new CustomInternalException("데이터 변환 중 내부적인 오류가 발생하였습니다.");
        }
    }
}