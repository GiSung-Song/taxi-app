package com.taxi.rideservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.rideservice.dto.DriverRegisterDto;
import com.taxi.rideservice.dto.DriverStatusUpdateDto;
import com.taxi.rideservice.dto.DriverUpdateDto;
import com.taxi.rideservice.enums.DriverStatus;
import com.taxi.rideservice.service.DriverService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(DriverController.class)
@AutoConfigureMockMvc(addFilters = false)
class DriverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DriverService driverService;

    @Test
    void 기사_정보_등록_테스트() throws Exception {
        DriverRegisterDto dto =
                new DriverRegisterDto(1L, "123가4567", 5, "소나타", "1234567890", "01012341234");

        willDoNothing().given(driverService).registerDriver(dto);

        mockMvc.perform(post("/api/driver/register")
                .content(new ObjectMapper().writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void 기사_정보_등록_valid() throws Exception {
        DriverRegisterDto dto = new DriverRegisterDto();

        dto.setUserId(1L);
        dto.setCarNumber("123가4567");
        dto.setCarName("소나타");
        dto.setLicense("1234123412");
        dto.setPhoneNumber("01012341234");

        mockMvc.perform(post("/api/driver/register")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 기사_정보_수정_테스트() throws Exception {
        DriverUpdateDto dto = new DriverUpdateDto();

        dto.setEmail("test@email.com");
        dto.setLicense("12341234");
        dto.setPhoneNumber("01012341234");
        dto.setCarNumber("123가5556");
        dto.setCapacity(10);
        dto.setCarName("카니발");

        willDoNothing().given(driverService).updateDriver(dto);

        mockMvc.perform(patch("/api/driver/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void 기사_정보_수정_valid() throws Exception {
        DriverUpdateDto dto = new DriverUpdateDto();

        dto.setEmail("test@email.com");
        dto.setLicense("12341234");
        dto.setPhoneNumber("01012341234");
        dto.setCarNumber("123가5556");
        dto.setCarName("카니발");

        mockMvc.perform(patch("/api/driver/info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 기사_상태_수정_테스트() throws Exception {
        DriverStatusUpdateDto dto = new DriverStatusUpdateDto("email@test.com", "DRIVING");

        willDoNothing().given(driverService).updateDriverStatus(dto);

        mockMvc.perform(patch("/api/driver/status")
                .content(new ObjectMapper().writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void 기사_상태_수정_valid() throws Exception {
        DriverStatusUpdateDto dto = new DriverStatusUpdateDto();
        dto.setEmail("email@email.com");

        mockMvc.perform(patch("/api/driver/status")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}