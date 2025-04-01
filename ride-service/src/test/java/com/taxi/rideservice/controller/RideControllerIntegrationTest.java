package com.taxi.rideservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.*;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.config.KafkaContainerConfig;
import com.taxi.rideservice.config.TestContainerConfig;
import com.taxi.rideservice.dto.*;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.entity.Ride;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.enums.RideStatus;
import com.taxi.rideservice.kafka.RideProducer;
import com.taxi.rideservice.repository.DriverRepository;
import com.taxi.rideservice.repository.RideRepository;
import com.taxi.rideservice.service.RideService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import({TestContainerConfig.class, KafkaContainerConfig.class})
@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
public class RideControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RideService rideService;

    @Autowired
    private RideProducer rideProducer;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private ConsumerFactory<String, String> consumerFactory;

    @MockBean
    private UserServiceClient userServiceClient;

    private CountDownLatch countDownLatch = new CountDownLatch(1);

    @BeforeEach
    void redis_초기화() {
        redisTemplate.opsForValue().set("key", "value");
    }

    @Test
    void 택시_호출_테스트() throws Exception {
        RideCallRequestDto dto = new RideCallRequestDto();

        dto.setPassengerEmail("passenger@email.com");
        dto.setStartLatitude(50.1);
        dto.setStartLongitude(50.1);
        dto.setStartLocation("start location");
        dto.setEndLatitude(51.1);
        dto.setEndLongitude(51.1);
        dto.setEndLocation("end location");

        mockMvc.perform(post("/api/ride/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andDo(print());

        countDownLatch.await(5, TimeUnit.SECONDS);

        String jsonValue = redisTemplate.opsForValue().get("ride:detail:passenger@email.com");
        RideCallRequestDto rideCallRequestDto = objectMapper.readValue(jsonValue, RideCallRequestDto.class);

        assertEquals(dto.getStartLocation(), rideCallRequestDto.getStartLocation());
        assertEquals(dto.getEndLocation(), rideCallRequestDto.getEndLocation());

        List<Point> position = redisTemplate.opsForGeo().position("ride:request", "passenger@email.com");

        double x = position.get(0).getX();
        double y = position.get(0).getY();

        double diff = 0.0001;

        assertNotNull(position);
        assertTrue(Math.abs(x - dto.getStartLongitude()) < diff);
        assertTrue(Math.abs(y - dto.getStartLatitude()) < diff);
    }

    @Test
    void 택시_호출_valid_실패_테스트() throws Exception {
        RideCallRequestDto dto = new RideCallRequestDto();

        dto.setPassengerEmail("passenger@email.com");
        dto.setStartLatitude(50.1);
        dto.setStartLongitude(50.1);
        dto.setStartLocation("start location");
        dto.setEndLatitude(51.1);
        dto.setEndLongitude(51.1);

        mockMvc.perform(post("/api/ride/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void 택시_호출_목록_테스트() throws Exception {
        RideCallRequestDto dto1 =
                new RideCallRequestDto("passenger1@email.com", 49.9998, 50.1110, "여기 어딘가1",
                        28.123123, 35.456456, "저기 어딘가1");

        RideCallRequestDto dto2 =
                new RideCallRequestDto("passenger2@email.com", 49.9997, 50.1109, "여기 어딘가2",
                        28.123123, 35.456456, "저기 어딘가2");

        RideCallRequestDto dto3 =
                new RideCallRequestDto("passenger3@email.com", 49.9996, 50.1108, "여기 어딘가3",
                        28.123123, 35.456456, "저기 어딘가3");

        RideCallRequestDto dto4 =
                new RideCallRequestDto("passenger4@email.com", 49.9995, 50.1107, "여기 어딘가4",
                        28.123123, 35.456456, "저기 어딘가4");


        redisTemplate.opsForGeo().add("ride:request", new Point(49.9998, 50.1110), "passenger1@email.com");
        redisTemplate.opsForGeo().add("ride:request", new Point(49.9997, 50.1109), "passenger2@email.com");
        redisTemplate.opsForGeo().add("ride:request", new Point(49.9996, 50.1108), "passenger3@email.com");
        redisTemplate.opsForGeo().add("ride:request", new Point(49.9995, 50.1107), "passenger4@email.com");

        redisTemplate.opsForValue().set("ride:detail:passenger1@email.com", objectMapper.writeValueAsString(dto1));
        redisTemplate.opsForValue().set("ride:detail:passenger2@email.com", objectMapper.writeValueAsString(dto2));
        redisTemplate.opsForValue().set("ride:detail:passenger3@email.com", objectMapper.writeValueAsString(dto3));
        redisTemplate.opsForValue().set("ride:detail:passenger4@email.com", objectMapper.writeValueAsString(dto4));

        FindCallRequestDto dto = new FindCallRequestDto();
        dto.setLongitude(49.9999);
        dto.setLatitude(50.1111);

        mockMvc.perform(post("/api/ride/find")
                .content(objectMapper.writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].startLocation").value(Matchers.hasItems("여기 어딘가1", "여기 어딘가2", "여기 어딘가3", "여기 어딘가4")))
                .andDo(print());
    }

    @Test
    void 택시_호출_수락_테스트() throws Exception {
        Driver driver = Driver.builder()
                .userId(1L)
                .carNumber("123GG1234")
                .carName("Carnival")
                .license("license1234")
                .capacity(15)
                .phoneNumber("01056785678")
                .totalRides(520)
                .driverStatus(DriverStatus.WAITING)
                .build();

        driverRepository.save(driver);

        UserDto passengerDto = new UserDto();
        UserDto driverDto = new UserDto();

        passengerDto.setUserId(0L);
        passengerDto.setName("passenger");
        passengerDto.setPhoneNumber("01012341234");

        driverDto.setUserId(1L);
        driverDto.setName("driver");
        driverDto.setPhoneNumber("01056785678");

        given(userServiceClient.getUserInfoByEmail("passenger@email.com")).willReturn(passengerDto);
        given(userServiceClient.getUserInfoByEmail("driver@email.com")).willReturn(driverDto);

        RideCallRequestDto rideCallRequestDto =
                new RideCallRequestDto("passenger1@email.com", 49.9998, 50.1110, "startLocation",
                        28.123123, 35.456456, "endLocation");

        redisTemplate.opsForGeo().add("ride:request", new Point(49.9998, 50.1110), "passenger@email.com");
        redisTemplate.opsForValue().set("ride:detail:passenger@email.com", objectMapper.writeValueAsString(rideCallRequestDto));

        CallAcceptRequestDto callAcceptRequestDto = new CallAcceptRequestDto();
        callAcceptRequestDto.setPassengerEmail("passenger@email.com");
        callAcceptRequestDto.setDriverEmail("driver@email.com");

        mockMvc.perform(post("/api/ride/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callAcceptRequestDto)))
                .andExpect(status().isOk())
                .andDo(print());

        Ride ride = rideRepository.findAll().get(0);

        assertEquals(rideCallRequestDto.getStartLocation(), ride.getStartLocation());
        assertEquals(rideCallRequestDto.getEndLocation(), ride.getEndLocation());
        assertEquals(DriverStatus.RESERVATION, driver.getDriverStatus());

        String jsonDetail = redisTemplate.opsForValue().get("ride:detail:passenger@email.com");
        assertNull(jsonDetail);

        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-accept"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            RideAcceptDto acceptDto = objectMapper.readValue(record.value(), RideAcceptDto.class);
            assertEquals("driver", acceptDto.getDriverName());
            assertEquals("startLocation", acceptDto.getStartLocation());
            assertEquals("endLocation", acceptDto.getEndLocation());
        }
    }

    @Test
    void 택시_호출_취소_테스트() throws Exception {
        Driver driver = Driver.builder()
                .userId(1L)
                .carNumber("123GG1234")
                .carName("Carnival")
                .license("license1234")
                .capacity(15)
                .phoneNumber("01056785678")
                .totalRides(520)
                .driverStatus(DriverStatus.RESERVATION)
                .build();

        driverRepository.save(driver);

        Ride ride = Ride.builder()
                .passengerId(2L)
                .driverId(driver.getId())
                .fare(0)
                .startLatitude(50.0)
                .startLongitude(50.0)
                .startLocation("startLocation")
                .endLatitude(51.1)
                .endLongitude(51.1)
                .endLocation("endLocation")
                .rideStatus(RideStatus.ACCEPT)
                .build();

        rideRepository.save(ride);

        UserDto driverDto = new UserDto();

        driverDto.setUserId(1L);
        driverDto.setName("driver");
        driverDto.setPhoneNumber("01056785678");

        given(userServiceClient.getUserInfoById(1L)).willReturn(driverDto);

        mockMvc.perform(post("/api/ride/cancel/{rideId}", ride.getId()))
                .andExpect(status().isOk())
                .andDo(print());

        assertEquals(RideStatus.CANCEL, ride.getRideStatus());
        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());

        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-cancel"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            RideCancelDto cancelDto = objectMapper.readValue(record.value(), RideCancelDto.class);
            assertEquals(RideStatus.CANCEL.name(), cancelDto.getRideStatus());
        }
    }

    @Test
    void 택시_출발_테스트() throws Exception {
        Driver driver = Driver.builder()
                .userId(1L)
                .carNumber("123GG1234")
                .carName("Carnival")
                .license("license1234")
                .capacity(15)
                .phoneNumber("01056785678")
                .totalRides(520)
                .driverStatus(DriverStatus.RESERVATION)
                .build();

        driverRepository.save(driver);

        Ride ride = Ride.builder()
                .passengerId(2L)
                .driverId(driver.getId())
                .fare(0)
                .startLatitude(50.0)
                .startLongitude(50.0)
                .startLocation("startLocation")
                .endLatitude(51.1)
                .endLongitude(51.1)
                .endLocation("endLocation")
                .rideStatus(RideStatus.ACCEPT)
                .build();

        rideRepository.save(ride);

        UserDto passengerDto = new UserDto();
        UserDto driverDto = new UserDto();

        passengerDto.setUserId(2L);
        passengerDto.setName("passenger");
        passengerDto.setPhoneNumber("01012341234");

        driverDto.setUserId(1L);
        driverDto.setName("driver");
        driverDto.setPhoneNumber("01056785678");

        given(userServiceClient.getUserInfoById(2L)).willReturn(passengerDto);
        given(userServiceClient.getUserInfoById(1L)).willReturn(driverDto);

        mockMvc.perform(post("/api/ride/start/{rideId}", ride.getId()))
                .andExpect(status().isOk())
                .andDo(print());

        assertEquals(RideStatus.DRIVING, ride.getRideStatus());
        assertEquals(DriverStatus.DRIVING, driver.getDriverStatus());

        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-start"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            RideStartDto rideStartDto = objectMapper.readValue(record.value(), RideStartDto.class);
            assertEquals(RideStatus.DRIVING.name(), rideStartDto.getRideStatus());
        }
    }

    @Test
    void 택시_운행_완료_테스트() throws Exception {
        Driver driver = Driver.builder()
                .userId(1L)
                .carNumber("123GG1234")
                .carName("Carnival")
                .license("license1234")
                .capacity(15)
                .phoneNumber("01056785678")
                .totalRides(520)
                .driverStatus(DriverStatus.DRIVING)
                .build();

        driverRepository.save(driver);

        Ride ride = Ride.builder()
                .passengerId(2L)
                .driverId(driver.getId())
                .fare(0)
                .startLatitude(50.0)
                .startLongitude(50.0)
                .startLocation("startLocation")
                .endLatitude(51.1)
                .endLongitude(51.1)
                .endLocation("endLocation")
                .rideStatus(RideStatus.DRIVING)
                .build();

        rideRepository.save(ride);

        UserDto passengerDto = new UserDto();
        UserDto driverDto = new UserDto();

        passengerDto.setUserId(2L);
        passengerDto.setName("passenger");
        passengerDto.setPhoneNumber("01012341234");

        driverDto.setUserId(1L);
        driverDto.setName("driver");
        driverDto.setPhoneNumber("01056785678");

        given(userServiceClient.getUserInfoById(2L)).willReturn(passengerDto);
        given(userServiceClient.getUserInfoById(1L)).willReturn(driverDto);

        RideCompleteDto dto = new RideCompleteDto();
        dto.setRideId(ride.getId());
        dto.setFare(50000);

        mockMvc.perform(post("/api/ride/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andDo(print());

        assertEquals(RideStatus.COMPLETE, ride.getRideStatus());
        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());
        assertEquals(50000, ride.getFare());

        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("ride-complete"));
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        for (ConsumerRecord<String, String> record : records) {
            DriveCompleteDto driveCompleteDto = objectMapper.readValue(record.value(), DriveCompleteDto.class);
            assertEquals(RideStatus.COMPLETE.name(), driveCompleteDto.getRideStatus());
            assertEquals(50000, driveCompleteDto.getFare());
        }
    }
}