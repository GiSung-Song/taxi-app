package com.taxi.notificationservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.exception.CustomInternalException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Slf4j
@Service
public class NotificationConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "ride-accept", groupId = "taxi-consumer-group")
    public void consumeRideAccept(String message) {
        try {
            log.info("Received Message : {}", message);


        } catch (Exception e) {
            throw new CustomInternalException("Json to Dto 변환 시 내부적인 오류가 발생하였습니다.");
        }
    }
}
