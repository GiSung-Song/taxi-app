package com.taxi.rideservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.DriveCompleteDto;
import com.taxi.common.core.dto.RideAcceptDto;
import com.taxi.common.core.dto.RideCancelDto;
import com.taxi.common.core.dto.RideStartDto;
import com.taxi.rideservice.config.KafkaContainerConfig;
import com.taxi.rideservice.config.TestContainerConfig;
import com.taxi.rideservice.dto.RideCallRequestDto;
import com.taxi.rideservice.dto.RideCompleteDto;
import com.taxi.rideservice.enums.RideStatus;
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
import java.time.LocalDateTime;
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

    @Test
    void 호출취소_producer_test() throws JsonProcessingException {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-cancel"));

        RideCancelDto rideCancelDto = new RideCancelDto();

        rideCancelDto.setRideId(1L);
        rideCancelDto.setRideStatus(RideStatus.CANCEL.name());
        rideCancelDto.setDriverUserId(1L);
        rideCancelDto.setCancelTime(LocalDateTime.now());

        rideProducer.sendRideCancel(rideCancelDto);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            RideCancelDto readValue = objectMapper.readValue(record.value(), RideCancelDto.class);
            assertEquals(rideCancelDto.getRideId(), readValue.getRideId());
            assertEquals(rideCancelDto.getRideStatus(), readValue.getRideStatus());
        }
    }

    @Test
    void 운행시작_producer_test() throws JsonProcessingException {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-start"));

        RideStartDto rideStartDto = new RideStartDto();

        rideStartDto.setRideId(1L);
        rideStartDto.setRideStatus(RideStatus.DRIVING.name());
        rideStartDto.setCarName("CAR NAME");
        rideStartDto.setStartLocation("start");
        rideStartDto.setEndLocation("end");

        rideProducer.sendRideStart(rideStartDto);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            RideStartDto readValue = objectMapper.readValue(record.value(), RideStartDto.class);
            assertEquals(rideStartDto.getRideId(), readValue.getRideId());
            assertEquals(rideStartDto.getRideStatus(), readValue.getRideStatus());
            assertEquals(rideStartDto.getStartLocation(), readValue.getStartLocation());
            assertEquals(rideStartDto.getEndLocation(), readValue.getEndLocation());
        }
    }

    @Test
    void 운행종료_producer_test() throws JsonProcessingException {
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-complete"));

        DriveCompleteDto driveCompleteDto = new DriveCompleteDto();

        driveCompleteDto.setRideId(1L);
        driveCompleteDto.setRideStatus(RideStatus.DRIVING.name());
        driveCompleteDto.setCarName("CAR NAME");
        driveCompleteDto.setStartLocation("start");
        driveCompleteDto.setEndLocation("end");
        driveCompleteDto.setFare(50000);

        rideProducer.sendRideComplete(driveCompleteDto);

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            DriveCompleteDto readValue = objectMapper.readValue(record.value(), DriveCompleteDto.class);
            assertEquals(driveCompleteDto.getRideId(), readValue.getRideId());
            assertEquals(driveCompleteDto.getRideStatus(), readValue.getRideStatus());
            assertEquals(driveCompleteDto.getStartLocation(), readValue.getStartLocation());
            assertEquals(driveCompleteDto.getEndLocation(), readValue.getEndLocation());
            assertEquals(driveCompleteDto.getFare(), readValue.getFare());
        }
    }
}