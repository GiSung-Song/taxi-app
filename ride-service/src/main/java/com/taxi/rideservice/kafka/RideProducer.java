package com.taxi.rideservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.RideAcceptDto;
import com.taxi.common.core.dto.RideCancelDto;
import com.taxi.common.core.dto.RideStartDto;
import com.taxi.common.core.exception.CustomInternalException;
import com.taxi.rideservice.dto.RideCallRequestDto;
import com.taxi.rideservice.dto.RideCompleteDto;
import com.taxi.rideservice.dto.RollBackDto;
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

    // 택시 호출 요청 producer
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

    // 택시 호출 수락 producer
    public void sendRideAccept(RideAcceptDto dto) {
        try {
            String rideInfo = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("ride-accept", rideInfo);
        } catch (JsonProcessingException e) {
            log.error("데이터 변환 중 내부적인 오류가 발생 : {}", e.getMessage());

            // 롤백 처리 (ride 삭제 및 운행 상태 변경)
            RollBackDto rollBackDto = new RollBackDto(dto.getRideId(), dto.getDriverUserId());

            rideService.rollBackRide(rollBackDto);

            throw new CustomInternalException("데이터 변환 중 내부적인 오류가 발생하였습니다.");
        }
    }

    // 택시 호출 취소 producer
    public void sendRideCancel(RideCancelDto dto) {
        try {
            String info = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("ride-cancel", info);
        } catch (JsonProcessingException e) {
            log.error("데이터 변환 중 내부적인 오류가 발생 : {}", e.getMessage());

            // 롤백 처리 (ride 삭제 및 운행 상태 변경)
            RollBackDto rollBackDto = new RollBackDto(dto.getRideId(), dto.getDriverUserId());

            rideService.rollBackRide(rollBackDto);

            throw new CustomInternalException("데이터 변환 중 내부적인 오류가 발생하였습니다.");
        }
    }

    // 택시 운행 시작 producer
    public void sendRideStart(RideStartDto dto) {
        try {
            String info = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("ride-start", info);
        } catch (JsonProcessingException e) {
            log.error("데이터 변환 중 내부적인 오류가 발생 : {}", e.getMessage());

            rideService.rollBackStartRide(dto);

            throw new CustomInternalException("데이터 변환 중 내부적인 오류가 발생하였습니다.");
        }
    }

    // 택시 운행 종료 producer
    public void sendRideComplete(RideCompleteDto dto) {
        try {
            String info = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("ride-complete", info);
        } catch (JsonProcessingException e) {
            log.error("데이터 변환 중 내부적인 오류가 발생 : {}", e.getMessage());

            rideService.rollBackCompleteRide(dto);

            throw new CustomInternalException("데이터 변환 중 내부적인 오류가 발생하였습니다.");
        }
    }
}