package com.taxi.rideservice.service;

import com.taxi.common.core.dto.UserDto;
import com.taxi.common.core.exception.CustomBadRequestException;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.config.TestContainerConfig;
import com.taxi.rideservice.dto.DriverRegisterDto;
import com.taxi.rideservice.dto.DriverStatusUpdateDto;
import com.taxi.rideservice.dto.DriverUpdateDto;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainerConfig.class)
public class DriverServiceIntegrationTest {

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @Test
    void 기사_정보_저장() {
        DriverRegisterDto dto =
                new DriverRegisterDto(1L, "123GG4567", 4, "KIA 1", "12903047382", "01012341234");

        driverService.registerDriver(dto);

        Driver driver = driverRepository.findByUserId(1L);

        assertNotNull(driver);
        assertEquals(dto.getCarNumber(), driver.getCarNumber());
        assertEquals(dto.getLicense(), driver.getLicense());
    }

    @Test
    void 기사_정보_저장_실패_중복_저장_테스트() {
        setUpDriver();

        DriverRegisterDto dto =
                new DriverRegisterDto(1L, "123GG4567", 4, "KIA 1", "12903047382", "01012341234");

        assertThrows(CustomBadRequestException.class, () -> driverService.registerDriver(dto));
    }

    @Test
    void 기사_정보_수정_테스트() {
        setUpDriver();
        UserDto userDto = setUpUserDto();
        given(userServiceClient.getUserInfoByEmail("test@email.com")).willReturn(userDto);

        DriverUpdateDto dto = new DriverUpdateDto();

        dto.setEmail("test@email.com");
        dto.setCapacity(15);
        dto.setPhoneNumber("01012341235");
        dto.setCarName("CARNIVAL 5");
        dto.setCarNumber("131AAA121");
        dto.setLicense("1231231231");

        driverService.updateDriver(dto);

        Driver driver = driverRepository.findByUserId(1L);

        assertEquals(dto.getCapacity(), driver.getCapacity());
        assertEquals(dto.getCarName(), driver.getCarName());
        assertEquals(dto.getCarNumber(), driver.getCarNumber());
    }

    @Test
    void 기사_정보_수정_실패_기사없음_테스트() {
        setUpDriver();
        UserDto userDto = setUpUserDto();
        userDto.setUserId(2L);

        given(userServiceClient.getUserInfoByEmail("test@email.com")).willReturn(userDto);

        DriverUpdateDto dto = new DriverUpdateDto();

        dto.setEmail("test@email.com");
        dto.setCapacity(15);
        dto.setPhoneNumber("01012341235");
        dto.setCarName("CARNIVAL 5");
        dto.setCarNumber("131AAA121");
        dto.setLicense("1231231231");

        assertThrows(CustomBadRequestException.class, () -> driverService.updateDriver(dto));
    }

    @Test
    void 기사_상태_수정_테스트() {
        setUpDriver();
        UserDto userDto = setUpUserDto();
        given(userServiceClient.getUserInfoByEmail("test@email.com")).willReturn(userDto);

        DriverStatusUpdateDto dto = new DriverStatusUpdateDto();

        dto.setEmail("test@email.com");
        dto.setDriverStatus("WAITING");

        driverService.updateDriverStatus(dto);

        Driver driver = driverRepository.findByUserId(1L);

        assertEquals(DriverStatus.valueOf(dto.getDriverStatus()), driver.getDriverStatus());
    }

    @Test
    void 기사_상태_수정_실패_기사없음_테스트() {
        setUpDriver();
        UserDto userDto = setUpUserDto();
        userDto.setUserId(2L);

        given(userServiceClient.getUserInfoByEmail("test@email.com")).willReturn(userDto);

        DriverStatusUpdateDto dto = new DriverStatusUpdateDto();

        dto.setEmail("test@email.com");
        dto.setDriverStatus("WAITING");

        assertThrows(CustomBadRequestException.class, () -> driverService.updateDriverStatus(dto));
    }

    private void setUpDriver() {
        Driver driver = Driver.builder()
                .userId(1L)
                .carNumber("123GG1234")
                .capacity(5)
                .carName("KIA 2")
                .license("1231231231")
                .phoneNumber("01012341234")
                .driverStatus(DriverStatus.OFFLINE)
                .totalRides(10)
                .build();

        driverRepository.save(driver);
    }

    private UserDto setUpUserDto() {
        UserDto userDto = new UserDto();

        userDto.setUserId(1L);
        userDto.setName("driver");
        userDto.setPhoneNumber("01012341234");

        return userDto;
    }

}
