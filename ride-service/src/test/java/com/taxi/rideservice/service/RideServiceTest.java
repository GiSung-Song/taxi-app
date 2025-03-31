package com.taxi.rideservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.UserDto;
import com.taxi.common.core.exception.CustomBadRequestException;
import com.taxi.common.core.exception.CustomInternalException;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.dto.*;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.entity.Ride;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.enums.RideStatus;
import com.taxi.rideservice.repository.DriverRepository;
import com.taxi.rideservice.repository.RideRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ActiveProfiles("h2")
@ExtendWith(MockitoExtension.class)
class RideServiceTest {

    @InjectMocks
    private RideService rideService;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Test
    void 택시_호출_테스트() throws JsonProcessingException {
        RideCallRequestDto dto =
                new RideCallRequestDto("test@email.com", 48.123123, 85.456456, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        String key = "ride:detail:" + dto.getPassengerEmail();
        String value = "{\"passengerEmail\":\"test@email.com\", \"startLatitude\":48.123123,\"startLongitude\":85.456456,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":28.123123,\"endLongitude\":35.456456,\"endLocation\":\"저기 어딘가\"";

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(objectMapper.writeValueAsString(dto)).thenReturn(value);

        rideService.saveCallRequest(dto);

        verify(geoOperations, times(1)).add(eq("ride:request"), any(Point.class), eq(dto.getPassengerEmail()));
        verify(valueOperations, times(1)).set(eq(key), eq(value));
    }

    @Test
    void 택시_호출_실패_테스트() throws JsonProcessingException {
        RideCallRequestDto dto =
                new RideCallRequestDto("test@email.com", 48.123123, 85.456456, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(objectMapper.writeValueAsString(dto)).thenThrow(JsonProcessingException.class);

        Assertions.assertThrows(CustomInternalException.class, () -> rideService.saveCallRequest(dto));
    }

    @Test
    void 택시_호출_목록_테스트() throws JsonProcessingException {
        FindCallRequestDto dto = new FindCallRequestDto(50.1, 49.1);

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String key1 = "ride:detail:email1@test.com";
        String value1 = "{\"passengerEmail\":\"email1@test.com\", \"startLatitude\":50.0,\"startLongitude\":49.0,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":100.0,\"endLongitude\":87.5,\"endLocation\":\"저기 어딘가\"";

        String key2 = "ride:detail:email2@test.com";
        String value2 = "{\"passengerEmail\":\"email2@test.com\", \"startLatitude\":50.2,\"startLongitude\":49.2,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":100.0,\"endLongitude\":87.5,\"endLocation\":\"저기 어딘가\"";

        RideCallRequestDto dto1 = new RideCallRequestDto("email1@test.com", 50.0, 49.0, "여기 어딘가", 100.0, 87.5, "저기 어딘가");
        RideCallRequestDto dto2 = new RideCallRequestDto("email2@test.com", 50.2, 49.2, "여기 어딘가", 100.0, 87.5, "저기 어딘가");

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = new GeoResults<>(List.of(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("email1@test.com", new Point(50.0, 49.0)), new Distance(3)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("email2@test.com", new Point(50.2, 49.2)), new Distance(4))
        ));

        when(geoOperations.radius(eq("ride:request"), any(Circle.class))).thenReturn(results);

        when(valueOperations.get(key1)).thenReturn(value1);
        when(valueOperations.get(key2)).thenReturn(value2);

        when(objectMapper.readValue(value1, RideCallRequestDto.class)).thenReturn(dto1);
        when(objectMapper.readValue(value2, RideCallRequestDto.class)).thenReturn(dto2);

        List<CallResponseDto> nearbyCall = rideService.findNearbyCall(dto);

        assertEquals(2, nearbyCall.size());
        assertEquals("email1@test.com", nearbyCall.get(0).getPassengerEmail());
        assertEquals("email2@test.com", nearbyCall.get(1).getPassengerEmail());
    }

    @Test
    void 택시_호출_목록_실패_테스트() throws JsonProcessingException {
        FindCallRequestDto dto = new FindCallRequestDto(50.1, 49.1);

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String key1 = "ride:detail:email1@test.com";
        String value1 = "{\"passengerEmail\":\"email1@test.com\", \"startLatitude\":50.0,\"startLongitude\":49.0,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":100.0,\"endLongitude\":87.5,\"endLocation\":\"저기 어딘가\"";

        String key2 = "ride:detail:email2@test.com";
        String value2 = "{\"passengerEmail\":\"email2@test.com\", \"startLatitude\":50.2,\"startLongitude\":49.2,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":100.0,\"endLongitude\":87.5,\"endLocation\":\"저기 어딘가\"";

        RideCallRequestDto dto1 = new RideCallRequestDto("email1@test.com", 50.0, 49.0, "여기 어딘가", 100.0, 87.5, "저기 어딘가");
        RideCallRequestDto dto2 = new RideCallRequestDto("email2@test.com", 50.2, 49.2, "여기 어딘가", 100.0, 87.5, "저기 어딘가");

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = new GeoResults<>(List.of(
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("email1@test.com", new Point(50.0, 49.0)), new Distance(3)),
                new GeoResult<>(new RedisGeoCommands.GeoLocation<>("email2@test.com", new Point(50.2, 49.2)), new Distance(4))
        ));

        when(geoOperations.radius(eq("ride:request"), any(Circle.class))).thenReturn(results);

        when(objectMapper.readValue(value1, RideCallRequestDto.class)).thenThrow(JsonProcessingException.class);

        assertThrows(CustomInternalException.class, () -> rideService.findNearbyCall(dto));
    }

    @Test
    void 호출_수락_테스트() throws JsonProcessingException {
        CallAcceptRequestDto acceptDto = new CallAcceptRequestDto("user@email.com", "driver@email.com");
        String jsonData = "{\"passengerEmail\":\"user@email.com\", \"startLatitude\":50.0,\"startLongitude\":49.0,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":100.0,\"endLongitude\":87.5,\"endLocation\":\"저기 어딘가\"";

        RideCallRequestDto callDto =
                new RideCallRequestDto("user@email.com", 50.0, 49.0, "여기 어딘가", 100.0, 87.5, "저기 어딘가");

        UserDto userDto = new UserDto(0L, "승객", "01012341234");
        UserDto driverDto = new UserDto(1L, "기사", "01056785678");

        Driver driver = Driver.builder()
                .userId(0L)
                .driverStatus(DriverStatus.WAITING)
                .build();

        when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(jsonData);
        when(objectMapper.readValue(jsonData, RideCallRequestDto.class)).thenReturn(callDto);
        when(userServiceClient.getUserInfoByEmail(anyString())).thenReturn(userDto).thenReturn(driverDto);
        when(driverRepository.findByUserId(anyLong())).thenReturn(driver);

        rideService.acceptCall(acceptDto);

        verify(rideRepository, times(1)).save(any(Ride.class));
        verify(redisTemplate, times(1)).delete("ride:detail:user@email.com");
        verify(redisTemplate.opsForGeo(), times(1)).remove("ride:request", "user@email.com");
    }

    @Test
    void 호출_수락_기사_없음_테스트() throws JsonProcessingException {
        CallAcceptRequestDto acceptDto = new CallAcceptRequestDto("user@email.com", "driver@email.com");
        String jsonData = "{\"passengerEmail\":\"user@email.com\", \"startLatitude\":50.0,\"startLongitude\":49.0,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":100.0,\"endLongitude\":87.5,\"endLocation\":\"저기 어딘가\"";

        RideCallRequestDto callDto =
                new RideCallRequestDto("user@email.com", 50.0, 49.0, "여기 어딘가", 100.0, 87.5, "저기 어딘가");

        UserDto userDto = new UserDto(0L, "승객", "01012341234");
        UserDto driverDto = new UserDto(1L, "기사", "01056785678");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(jsonData);
        when(objectMapper.readValue(jsonData, RideCallRequestDto.class)).thenReturn(callDto);
        when(userServiceClient.getUserInfoByEmail(anyString())).thenReturn(userDto).thenReturn(driverDto);
        when(driverRepository.findByUserId(anyLong())).thenReturn(null);

        assertThrows(CustomBadRequestException.class, () -> rideService.acceptCall(acceptDto));
    }

    @Test
    void 호출_수락_기사_상태_대기중아님_테스트() throws JsonProcessingException {
        CallAcceptRequestDto acceptDto = new CallAcceptRequestDto("user@email.com", "driver@email.com");
        String jsonData = "{\"passengerEmail\":\"user@email.com\", \"startLatitude\":50.0,\"startLongitude\":49.0,\"startLocation\":\"여기 어딘가\"" +
                ",\"endLatitude\":100.0,\"endLongitude\":87.5,\"endLocation\":\"저기 어딘가\"";

        RideCallRequestDto callDto =
                new RideCallRequestDto("user@email.com", 50.0, 49.0, "여기 어딘가", 100.0, 87.5, "저기 어딘가");

        UserDto userDto = new UserDto(0L, "승객", "01012341234");
        UserDto driverDto = new UserDto(1L, "기사", "01056785678");

        Driver driver = Driver.builder()
                .userId(0L)
                .userId(1L)
                .driverStatus(DriverStatus.OFFLINE)
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForValue().get(anyString())).thenReturn(jsonData);
        when(objectMapper.readValue(jsonData, RideCallRequestDto.class)).thenReturn(callDto);
        when(userServiceClient.getUserInfoByEmail(anyString())).thenReturn(userDto).thenReturn(driverDto);
        when(driverRepository.findByUserId(anyLong())).thenReturn(driver);

        assertThrows(CustomBadRequestException.class, () -> rideService.acceptCall(acceptDto));
    }

    @Test
    void 호출_취소_테스트() {
        Ride ride = Ride.builder()
                .id(0L)
                .passengerId(2L)
                .driverId(0L)
                .rideStatus(RideStatus.ACCEPT)
                .build();

        Driver driver = Driver.builder()
                .id(0L)
                .driverStatus(DriverStatus.RESERVATION)
                .build();

        when(rideRepository.findById(anyLong())).thenReturn(Optional.of(ride));
        when(driverRepository.findById(anyLong())).thenReturn(Optional.of(driver));

        rideService.cancelRide(0L);

        assertEquals(RideStatus.CANCEL, ride.getRideStatus());
        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());
    }

    @Test
    void 호출_취소_상태_불가_테스트() {
        Ride ride = Ride.builder()
                .id(0L)
                .passengerId(2L)
                .driverId(0L)
                .rideStatus(RideStatus.DRIVING)
                .build();

        Driver driver = Driver.builder()
                .id(0L)
                .driverStatus(DriverStatus.RESERVATION)
                .build();

        when(rideRepository.findById(anyLong())).thenReturn(Optional.of(ride));
        when(driverRepository.findById(anyLong())).thenReturn(Optional.of(driver));

        assertThrows(CustomBadRequestException.class, () -> rideService.cancelRide(0L));
    }

    @Test
    void 운행_시작_테스트() {
        Ride ride = Ride.builder()
                .id(0L)
                .passengerId(2L)
                .driverId(0L)
                .rideStatus(RideStatus.ACCEPT)
                .build();

        Driver driver = Driver.builder()
                .id(0L)
                .driverStatus(DriverStatus.RESERVATION)
                .build();

        when(rideRepository.findById(anyLong())).thenReturn(Optional.of(ride));
        when(driverRepository.findById(anyLong())).thenReturn(Optional.of(driver));

        rideService.startRide(0L);

        assertEquals(RideStatus.DRIVING, ride.getRideStatus());
        assertEquals(DriverStatus.DRIVING, driver.getDriverStatus());
    }

    @Test
    void 운행_종료_테스트() {
        Ride ride = Ride.builder()
                .id(0L)
                .passengerId(2L)
                .driverId(0L)
                .rideStatus(RideStatus.DRIVING)
                .build();

        Driver driver = Driver.builder()
                .id(0L)
                .driverStatus(DriverStatus.DRIVING)
                .totalRides(5)
                .build();

        RideCompleteDto dto = new RideCompleteDto(5000, 0L);

        when(rideRepository.findById(anyLong())).thenReturn(Optional.of(ride));
        when(driverRepository.findById(anyLong())).thenReturn(Optional.of(driver));

        rideService.completeRide(dto);

        assertEquals(5000, ride.getFare());
        assertEquals(RideStatus.COMPLETE, ride.getRideStatus());
        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());
        assertEquals(6, driver.getTotalRides());
    }

    @Test
    void 롤백_테스트() {
        Ride ride = Ride.builder()
                .id(0L)
                .passengerId(2L)
                .driverId(0L)
                .rideStatus(RideStatus.DRIVING)
                .build();

        Driver driver = Driver.builder()
                .id(0L)
                .userId(1L)
                .driverStatus(DriverStatus.DRIVING)
                .totalRides(5)
                .build();

        when(rideRepository.findById(anyLong())).thenReturn(Optional.of(ride));
        when(driverRepository.findByUserId(anyLong())).thenReturn(driver);

        RollBackDto dto = new RollBackDto();
        dto.setRideId(0L);
        dto.setDriverUserId(1L);

        rideService.rollBackRide(dto);

        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());
        verify(rideRepository, times(1)).delete(any(Ride.class));
    }
}