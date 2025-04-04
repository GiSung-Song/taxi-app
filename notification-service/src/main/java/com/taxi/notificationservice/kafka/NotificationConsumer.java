package com.taxi.notificationservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.DriveCompleteDto;
import com.taxi.common.core.dto.RideAcceptDto;
import com.taxi.common.core.dto.RideCancelDto;
import com.taxi.common.core.dto.RideStartDto;
import com.taxi.common.core.exception.CustomInternalException;
import com.taxi.notificationservice.dto.*;
import com.taxi.notificationservice.dto.mapper.RideAcceptMapper;
import com.taxi.notificationservice.dto.mapper.RideCancelMapper;
import com.taxi.notificationservice.dto.mapper.RideCompleteMapper;
import com.taxi.notificationservice.dto.mapper.RideStartMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class NotificationConsumer {

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "ride-accept", groupId = "taxi-consumer-group")
    public void consumeRideAccept(String message) {
        try {
            log.info("Received Message : {}", message);

            RideAcceptDto rideAcceptDto = objectMapper.readValue(message, RideAcceptDto.class);

            // 승객 데이터(기사 정보, 출발지, 도착지 등) 승객에게 전송
            PassengerAcceptDto passengerAcceptDto = RideAcceptMapper.toPassengerAcceptDto(rideAcceptDto);
            String passengerData = objectMapper.writeValueAsString(passengerAcceptDto);
            messagingTemplate.convertAndSendToUser(rideAcceptDto.getPassengerEmail(), "/queue/notification", passengerData);

            // 기사 데이터(승객 전화번호, 출발지, 도착지 등) 기사에게 전송
            DriverAcceptDto driverAcceptDto = RideAcceptMapper.toDriverAcceptDto(rideAcceptDto);
            String driverData = objectMapper.writeValueAsString(driverAcceptDto);
            messagingTemplate.convertAndSendToUser(rideAcceptDto.getDriverEmail(), "/queue/notification", driverData);
        } catch (Exception e) {
            log.error("Json to Dto 변환 시 내부적인 오류가 발생하였습니다. : {}", e.getMessage());

            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }
    }

    @KafkaListener(topics = "ride-cancel", groupId = "taxi-consumer-group")
    public void consumeRideCancel(String message) {
        try {
            log.info("Received Message : {}", message);

            RideCancelDto rideCancelDto = objectMapper.readValue(message, RideCancelDto.class);

            // 승객 데이터 승객에게 전송
            PassengerCancelDto passengerCancelDto = RideCancelMapper.toPassengerCancelDto(rideCancelDto);
            String passengerData = objectMapper.writeValueAsString(passengerCancelDto);
            messagingTemplate.convertAndSendToUser(rideCancelDto.getPassengerEmail(), "/queue/notification", passengerData);

            // 기사 데이터 기사에게 전송
            DriverCancelDto driverCancelDto = RideCancelMapper.toDriverCancelDto(rideCancelDto);
            String driverData = objectMapper.writeValueAsString(driverCancelDto);
            messagingTemplate.convertAndSendToUser(rideCancelDto.getDriverEmail(), "/queue/notification", driverData);
        } catch (Exception e) {
            log.error("Json to Dto 변환 시 내부적인 오류가 발생하였습니다. : {}", e.getMessage());

            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }
    }

    @KafkaListener(topics = "ride-start", groupId = "taxi-consumer-group")
    public void consumeRideStart(String message) {
        try {
            log.info("Received Message : {}", message);

            RideStartDto rideStartDto = objectMapper.readValue(message, RideStartDto.class);

            // 승객 데이터 승객에게 전송
            PassengerStartDto passengerStartDto = RideStartMapper.toPassengerStartDto(rideStartDto);
            String passengerData = objectMapper.writeValueAsString(passengerStartDto);
            messagingTemplate.convertAndSendToUser(rideStartDto.getPassengerEmail(), "/queue/notification", passengerData);

            // 기사 데이터 기사에게 전송
            DriverStartDto driverStartDto = RideStartMapper.toDriverStartDto(rideStartDto);
            String driverData = objectMapper.writeValueAsString(driverStartDto);
            messagingTemplate.convertAndSendToUser(rideStartDto.getDriverEmail(), "/queue/notification", driverData);
        } catch (Exception e) {
            log.error("Json to Dto 변환 시 내부적인 오류가 발생하였습니다. : {}", e.getMessage());

            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }
    }

    @KafkaListener(topics = "ride-complete", groupId = "taxi-consumer-group")
    public void consumeRideComplete(String message) {
        try {
            log.info("Received Message : {}", message);

            DriveCompleteDto driveCompleteDto = objectMapper.readValue(message, DriveCompleteDto.class);

            // 승객 데이터 승객에게 전송
            PassengerCompleteDto passengerCompleteDto = RideCompleteMapper.toPassengerCompleteDto(driveCompleteDto);
            String passengerData = objectMapper.writeValueAsString(passengerCompleteDto);
            messagingTemplate.convertAndSendToUser(driveCompleteDto.getPassengerEmail(), "/queue/notification", passengerData);

            // 기사 데이터 기사에게 전송
            DriverCompleteDto driverCompleteDto = RideCompleteMapper.toDriverCompleteDto(driveCompleteDto);
            String driverData = objectMapper.writeValueAsString(driverCompleteDto);
            messagingTemplate.convertAndSendToUser(driveCompleteDto.getDriverEmail(), "/queue/notification", driverData);
        } catch (Exception e) {
            log.error("Json to Dto 변환 시 내부적인 오류가 발생하였습니다. : {}", e.getMessage());

            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }
    }
}
