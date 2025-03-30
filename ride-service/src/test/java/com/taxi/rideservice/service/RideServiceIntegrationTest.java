package com.taxi.rideservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.UserDto;
import com.taxi.common.core.exception.CustomBadRequestException;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.config.TestContainerConfig;
import com.taxi.rideservice.dto.*;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.entity.Ride;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.enums.RideStatus;
import com.taxi.rideservice.repository.DriverRepository;
import com.taxi.rideservice.repository.RideRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
public class RideServiceIntegrationTest {

    @Autowired
    private RideService rideService;
    
    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private DriverRepository driverRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String GEO_KEY = "ride:request";
    private String DETAIL_KEY_PREFIX = "ride:detail:";

    @Test
    void 택시_호출_테스트() throws JsonProcessingException {
        RideCallRequestDto dto =
                new RideCallRequestDto("test@email.com", 48.123123, 85.456456, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");
        
        rideService.saveCallRequest(dto);

        String key = DETAIL_KEY_PREFIX + dto.getPassengerEmail();
        String value = redisTemplate.opsForValue().get(key);
        RideCallRequestDto storedValue = objectMapper.readValue(value, RideCallRequestDto.class);

        assertNotNull(value);
        assertEquals(dto.getStartLocation(), storedValue.getStartLocation());
        assertEquals(dto.getEndLocation(), storedValue.getEndLocation());
    }

    @Test
    void 택시_호출_목록_조회_테스트() throws JsonProcessingException {
        FindCallRequestDto dto = new FindCallRequestDto();
        dto.setLongitude(49.9999);
        dto.setLatitude(50.1111);

        RideCallRequestDto dto1 =
                new RideCallRequestDto("test1@email.com", 49.9998, 50.1110, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        RideCallRequestDto dto2 =
                new RideCallRequestDto("test2@email.com", 49.9997, 50.1109, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        RideCallRequestDto dto3 =
                new RideCallRequestDto("test3@email.com", 49.9996, 50.1108, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        RideCallRequestDto dto4 =
                new RideCallRequestDto("test4@email.com", 49.9995, 50.1107, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        redisTemplate.opsForGeo().add(GEO_KEY, new Point(49.9998, 50.1110), "test1@email.com");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(49.9997, 50.1109), "test2@email.com");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(49.9996, 50.1108), "test3@email.com");
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(49.9995, 50.1107), "test4@email.com");

        redisTemplate.opsForValue().set(DETAIL_KEY_PREFIX + "test1@email.com", objectMapper.writeValueAsString(dto1));
        redisTemplate.opsForValue().set(DETAIL_KEY_PREFIX + "test2@email.com", objectMapper.writeValueAsString(dto2));
        redisTemplate.opsForValue().set(DETAIL_KEY_PREFIX + "test3@email.com", objectMapper.writeValueAsString(dto3));
        redisTemplate.opsForValue().set(DETAIL_KEY_PREFIX + "test4@email.com", objectMapper.writeValueAsString(dto4));

        List<CallResponseDto> nearbyCall = rideService.findNearbyCall(dto);

        assertNotNull(nearbyCall);
        assertEquals(4, nearbyCall.size());
    }

    @Test
    void 택시_호출_수락_테스트() throws JsonProcessingException {
        RideCallRequestDto callRequestDto =
                new RideCallRequestDto("passenger@email.com", 49.9998, 50.1110, "LA 1 Street",
                        28.123123, 35.456456, "NY 523 Street");

        redisTemplate.opsForValue().set(DETAIL_KEY_PREFIX + "passenger@email.com", objectMapper.writeValueAsString(callRequestDto));
        redisTemplate.opsForGeo().add(GEO_KEY, new Point(49.9998, 50.1110), "passenger@email.com");

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

        CallAcceptRequestDto dto = new CallAcceptRequestDto();

        dto.setPassengerEmail("passenger@email.com");
        dto.setDriverEmail("driver@email.com");

        RideInfoDto rideInfoDto = rideService.acceptCall(dto);

        assertNotNull(rideInfoDto);
        assertEquals(callRequestDto.getStartLocation(), rideInfoDto.getStartLocation());
        assertEquals(callRequestDto.getEndLocation(), rideInfoDto.getEndLocation());
    }

    @Test
    void 운행_취소_테스트() {
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
                .passengerId(0L)
                .driverId(driver.getId())
                .fare(5000)
                .startLatitude(50.1)
                .startLongitude(50.0)
                .startLocation("start")
                .endLatitude(50.2)
                .endLongitude(50.3)
                .endLocation("end")
                .rideStatus(RideStatus.ACCEPT)
                .build();

        rideRepository.save(ride);

        rideService.cancelRide(ride.getId());

        assertEquals(RideStatus.CANCEL, ride.getRideStatus());
        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());
    }

    @Test
    void 운행_취소_실패_상태이상_테스트() {
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
                .passengerId(0L)
                .driverId(driver.getId())
                .fare(5000)
                .startLatitude(50.1)
                .startLongitude(50.0)
                .startLocation("start")
                .endLatitude(50.2)
                .endLongitude(50.3)
                .endLocation("end")
                .rideStatus(RideStatus.DRIVING)
                .build();

        rideRepository.save(ride);

        assertThrows(CustomBadRequestException.class, () -> rideService.cancelRide(ride.getId()));
    }

    @Test
    void 운행_시작_테스트() {
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
                .passengerId(0L)
                .driverId(driver.getId())
                .fare(5000)
                .startLatitude(50.1)
                .startLongitude(50.0)
                .startLocation("start")
                .endLatitude(50.2)
                .endLongitude(50.3)
                .endLocation("end")
                .rideStatus(RideStatus.ACCEPT)
                .build();

        rideRepository.save(ride);

        rideService.startRide(ride.getId());

        assertEquals(DriverStatus.DRIVING, driver.getDriverStatus());
        assertEquals(RideStatus.DRIVING, ride.getRideStatus());
    }

    @Test
    void 운행_시작_실패_운행상태_테스트() {
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
                .passengerId(0L)
                .driverId(driver.getId())
                .fare(5000)
                .startLatitude(50.1)
                .startLongitude(50.0)
                .startLocation("start")
                .endLatitude(50.2)
                .endLongitude(50.3)
                .endLocation("end")
                .rideStatus(RideStatus.CANCEL)
                .build();

        rideRepository.save(ride);

        assertThrows(CustomBadRequestException.class, () -> rideService.startRide(ride.getId()));
    }

    @Test
    void 운행_시작_실패_기사상태_테스트() {
        Driver driver = Driver.builder()
                .userId(1L)
                .carNumber("123GG1234")
                .carName("Carnival")
                .license("license1234")
                .capacity(15)
                .phoneNumber("01056785678")
                .totalRides(520)
                .driverStatus(DriverStatus.OFFLINE)
                .build();

        driverRepository.save(driver);

        Ride ride = Ride.builder()
                .passengerId(0L)
                .driverId(driver.getId())
                .fare(5000)
                .startLatitude(50.1)
                .startLongitude(50.0)
                .startLocation("start")
                .endLatitude(50.2)
                .endLongitude(50.3)
                .endLocation("end")
                .rideStatus(RideStatus.ACCEPT)
                .build();

        rideRepository.save(ride);

        assertThrows(CustomBadRequestException.class, () -> rideService.startRide(ride.getId()));
    }

    @Test
    void 운행_완료_테스트() {
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
                .passengerId(0L)
                .driverId(driver.getId())
                .fare(5000)
                .startLatitude(50.1)
                .startLongitude(50.0)
                .startLocation("start")
                .endLatitude(50.2)
                .endLongitude(50.3)
                .endLocation("end")
                .rideStatus(RideStatus.DRIVING)
                .build();

        rideRepository.save(ride);

        RideCompleteDto dto = new RideCompleteDto();

        dto.setFare(20000);
        dto.setRideId(ride.getId());

        rideService.completeRide(dto);

        assertEquals(20000, ride.getFare());
        assertEquals(RideStatus.COMPLETE, ride.getRideStatus());
        assertEquals(521, driver.getTotalRides());
        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());
    }
}
