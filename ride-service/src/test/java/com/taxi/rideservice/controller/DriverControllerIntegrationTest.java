package com.taxi.rideservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.UserDto;
import com.taxi.rideservice.client.UserServiceClient;
import com.taxi.rideservice.config.TestContainerConfig;
import com.taxi.rideservice.dto.DriverRegisterDto;
import com.taxi.rideservice.dto.DriverStatusUpdateDto;
import com.taxi.rideservice.dto.DriverUpdateDto;
import com.taxi.rideservice.entity.Driver;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.repository.DriverRepository;
import com.taxi.rideservice.service.DriverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@Import(TestContainerConfig.class)
@SpringBootTest
@Transactional
@AutoConfigureMockMvc(addFilters = false)
public class DriverControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DriverService driverService;

    @Autowired
    private DriverRepository driverRepository;

    @MockBean
    private UserServiceClient userServiceClient;

    @Test
    void 기사_정보_등록_테스트() throws Exception {
        DriverRegisterDto dto =
                new DriverRegisterDto(1L, "123GG4567", 4, "KIA 1", "12903047382", "01012341234");

        mockMvc.perform(post("/api/driver/register")
                .content(new ObjectMapper().writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());

        Driver driver = driverRepository.findAll().get(0);

        assertEquals(dto.getCarNumber(), driver.getCarNumber());
        assertEquals(dto.getLicense(), driver.getLicense());
    }

    @Test
    void 기사_정보_등록_valid_실패_테스트() throws Exception {
        DriverRegisterDto dto =
                new DriverRegisterDto(1L, "123GG453243243267", 4, "KIA 1", "12903047382", "01012341234");

        mockMvc.perform(post("/api/driver/register")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void 기사_정보_수정_테스트() throws Exception {
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

        DriverUpdateDto dto = new DriverUpdateDto();
        dto.setEmail("driver@email.com");
        dto.setLicense("license5678");
        dto.setCarNumber("111AA2222");
        dto.setCapacity(5);
        dto.setCarName("K7");
        dto.setPhoneNumber("01056785678");

        UserDto userDto = new UserDto();
        userDto.setUserId(1L);
        userDto.setPhoneNumber("01056785678");
        userDto.setName("driver");

        given(userServiceClient.getUserInfoByEmail(anyString())).willReturn(userDto);

        mockMvc.perform(patch("/api/driver/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andDo(print());

        assertEquals(dto.getCarNumber(), driver.getCarNumber());
        assertEquals(dto.getLicense(), driver.getLicense());
        assertEquals(dto.getPhoneNumber(), driver.getPhoneNumber());
    }

    @Test
    void 기사_정보_수정_valid_실패_테스트() throws Exception {
        DriverUpdateDto dto = new DriverUpdateDto();
        dto.setEmail("driver@email.com");
        dto.setLicense("license5678");
        dto.setCarNumber("111AA2222");
        dto.setCapacity(5);
        dto.setCarName("K7");

        mockMvc.perform(patch("/api/driver/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    void 기사_상태_수정_테스트() throws Exception {
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

        DriverStatusUpdateDto dto = new DriverStatusUpdateDto();
        dto.setEmail("driver@email.com");
        dto.setDriverStatus(DriverStatus.OFFLINE.name());

        UserDto userDto = new UserDto();
        userDto.setUserId(1L);
        userDto.setPhoneNumber("01056785678");
        userDto.setName("driver");

        given(userServiceClient.getUserInfoByEmail(anyString())).willReturn(userDto);

        mockMvc.perform(patch("/api/driver/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andDo(print());

        assertEquals(DriverStatus.OFFLINE, driver.getDriverStatus());
    }

    @Test
    void 기사_상태_수정_valid_실패_테스트() throws Exception {
        DriverStatusUpdateDto dto = new DriverStatusUpdateDto();
        dto.setDriverStatus(DriverStatus.OFFLINE.name());

        mockMvc.perform(patch("/api/driver/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }
}
