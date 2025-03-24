package com.taxi.rideservice.service;

import com.taxi.common.dto.UserDto;
import com.taxi.common.exception.CustomBadRequestException;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.dto.DriverRegisterDto;
import com.taxi.rideservice.dto.DriverStatusUpdateDto;
import com.taxi.rideservice.dto.DriverUpdateDto;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("h2")
class DriverServiceTest {

    @InjectMocks
    private DriverService driverService;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Test
    void 기사_정보_저장() {
        DriverRegisterDto dto =
                new DriverRegisterDto(1L, "123가4567", 4, "아반떼 N", "12903047382", "01012341234");

        driverService.registerDriver(dto);

        verify(driverRepository, times(1)).save(argThat(driver ->
                driver.getLicense().equals(dto.getLicense()) &&
                driver.getCarNumber().equals(dto.getCarNumber()) &&
                driver.getCarName().equals(dto.getCarName())));
    }

    @Test
    void 기사_정보_저장_실패_중복() {
        DriverRegisterDto dto =
                new DriverRegisterDto(1L, "123가4567", 4, "아반떼 N", "12903047382", "01012341234");

        Driver driver = Driver.builder()
                .build();

        when(driverRepository.findByUserId(1L)).thenReturn(driver);

        assertThrows(CustomBadRequestException.class, () -> driverService.registerDriver(dto));
    }

    @Test
    void 기사_정보_수정() {
        Driver driver = Driver.builder()
                .carName("아반떼 N")
                .carNumber("123가4567")
                .capacity(4)
                .license("12903047382")
                .phoneNumber("01012341234")
                .userId(1L)
                .build();

        UserDto userDto = new UserDto(1L, "기사", "01012341234");

        DriverUpdateDto dto =
                new DriverUpdateDto("test@email.com", "123나4567", 10, "카니발", "12903047382", "01012341234");

        when(userServiceClient.getUserInfoByEmail(anyString())).thenReturn(userDto);
        when(driverRepository.findByUserId(anyLong())).thenReturn(driver);

        driverService.updateDriver(dto);

        assertEquals(dto.getCarNumber(), driver.getCarNumber());
        assertEquals(dto.getCapacity(), driver.getCapacity());
        assertEquals(dto.getCarName(), driver.getCarName());
    }

    @Test
    void 기사_정보_수정_실패() {
        UserDto userDto = new UserDto(1L, "기사", "01012341234");

        DriverUpdateDto dto =
                new DriverUpdateDto("test@email.com", "123나4567", 10, "카니발", "12903047382", "01012341234");

        when(userServiceClient.getUserInfoByEmail(anyString())).thenReturn(userDto);
        when(driverRepository.findByUserId(anyLong())).thenReturn(null);

        assertThrows(CustomBadRequestException.class, () -> driverService.updateDriver(dto));
    }

    @Test
    void 기사_운행_상태_수정() {
        Driver driver = Driver.builder()
                .carName("아반떼 N")
                .carNumber("123가4567")
                .capacity(4)
                .license("12903047382")
                .phoneNumber("01012341234")
                .userId(1L)
                .driverStatus(DriverStatus.OFFLINE)
                .build();

        UserDto userDto = new UserDto(1L, "기사", "01012341234");
        DriverStatusUpdateDto dto = new DriverStatusUpdateDto("email@test.com", "WAITING");

        when(userServiceClient.getUserInfoByEmail(anyString())).thenReturn(userDto);
        when(driverRepository.findByUserId(anyLong())).thenReturn(driver);

        driverService.updateDriverStatus(dto);

        assertEquals(DriverStatus.WAITING, driver.getDriverStatus());
    }

    @Test
    void 기사_운행_상태_수정_실패() {
        UserDto userDto = new UserDto(1L, "기사", "01012341234");
        DriverStatusUpdateDto dto = new DriverStatusUpdateDto("email@test.com", "WAITING");

        when(userServiceClient.getUserInfoByEmail(anyString())).thenReturn(userDto);
        when(driverRepository.findByUserId(anyLong())).thenReturn(null);

        assertThrows(CustomBadRequestException.class, () -> driverService.updateDriverStatus(dto));
    }

}