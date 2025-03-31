package com.taxi.rideservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.RideAcceptDto;
import com.taxi.rideservice.config.KafkaContainerConfig;
import com.taxi.rideservice.config.TestContainerConfig;
import com.taxi.rideservice.dto.RideCallRequestDto;
import com.taxi.rideservice.service.RideService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Import({KafkaContainerConfig.class, TestContainerConfig.class})
public class KafkaIntegrationTest {

    @Autowired
    private RideProducer rideProducer;

    @Autowired
    private RideConsumer rideConsumer;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RideService rideService;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    private CountDownLatch latch = new CountDownLatch(1);

    @Test
    void 호출_producer_consumer_테스트() throws JsonProcessingException, InterruptedException {
        RideCallRequestDto dto =
                new RideCallRequestDto("test@email.com", 48.123123, 85.456456, "start",
                        28.123123, 35.456456, "end");

        rideProducer.sendRideRequest(dto);

        redisTemplate.opsForValue().set("key", "value");

        List<Point> position = redisTemplate.opsForGeo().position("ride:request", "test@email.com");
        String jsonData = redisTemplate.opsForValue().get("ride:detail:test@email.com");

        System.out.println("jsonData = " + jsonData);

        RideCallRequestDto callRequestDto = objectMapper.readValue(jsonData, RideCallRequestDto.class);

        assertEquals(callRequestDto.getStartLocation(), dto.getStartLocation());
        assertEquals(1, position.size());
    }

    @Test
    void 탑승정보_producer_테스트() throws JsonProcessingException {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-accept"));

        RideAcceptDto rideInfoDto = new RideAcceptDto();

        rideInfoDto.setRideId(0L);
        rideInfoDto.setPassengerUserId(1L);
        rideInfoDto.setPassengerPhoneNumber("01012341234");
        rideInfoDto.setStartLocation("출발지");
        rideInfoDto.setEndLocation("도착지");

        rideInfoDto.setDriverUserId(1L);
        rideInfoDto.setDriverName("기사");
        rideInfoDto.setDriverPhoneNumber("01023452345");
        rideInfoDto.setCarName("코란도");
        rideInfoDto.setCarNumber("123가1234");
        rideInfoDto.setCapacity(4);
        rideInfoDto.setTotalRides(50);

        rideProducer.sendRideAccept(rideInfoDto);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            RideAcceptDto acceptDto = objectMapper.readValue(record.value(), RideAcceptDto.class);
            assertEquals(acceptDto.getStartLocation(), rideInfoDto.getStartLocation());
            assertEquals(acceptDto.getDriverName(), rideInfoDto.getDriverName());
        }

    }
}