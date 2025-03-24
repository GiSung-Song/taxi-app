package com.taxi.rideservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.rideservice.dto.*;
import com.taxi.rideservice.kafka.RideProducer;
import com.taxi.rideservice.service.RideService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(RideController.class)
@AutoConfigureMockMvc(addFilters = false)
class RideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RideService rideService;

    @MockBean
    private RideProducer rideProducer;

    @Test
    void 택시_호출_테스트() throws Exception {
        RideCallRequestDto dto =
                new RideCallRequestDto("test@email.com", 48.123123, 85.456456, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        willDoNothing().given(rideProducer).sendRideRequest(dto);

        mockMvc.perform(post("/api/ride/call")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isOk());

        verify(rideProducer, times(1)).sendRideRequest(any());
    }

    @Test
    void 택시_호출_valid() throws Exception {
        RideCallRequestDto dto =
                new RideCallRequestDto("test@email.com", 84563.1598, 85.456456, "여기 어딘가",
                        28.123123, 35.456456, "저기 어딘가");

        mockMvc.perform(post("/api/ride/call")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 택시_호출_목록_테스트() throws Exception {
        FindCallRequestDto dto = new FindCallRequestDto();

        dto.setLatitude(50.1234);
        dto.setLongitude(89.1234);

        List<CallResponseDto> callDto = new ArrayList<>();

        CallResponseDto response1 = new CallResponseDto("test1@email.com", "출발지1", "도착지1");
        CallResponseDto response2 = new CallResponseDto("test2@email.com", "출발지2", "도착지2");

        callDto.add(response1);
        callDto.add(response2);

        given(rideService.findNearbyCall(any())).willReturn(callDto);

        mockMvc.perform(post("/api/ride/find")
                .content(new ObjectMapper().writeValueAsString(dto))
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].startLocation").value("출발지1"))
                .andExpect(jsonPath("$.data[1].startLocation").value("출발지2"));
    }

    @Test
    void 택시_호출_목록_valid() throws Exception {
        FindCallRequestDto dto = new FindCallRequestDto();

        dto.setLatitude(50.1234);

        mockMvc.perform(post("/api/ride/find")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 택시_호출_수락_테스트() throws Exception {
        CallAcceptRequestDto callAcceptRequestDto = new CallAcceptRequestDto();
        callAcceptRequestDto.setDriverEmail("driver@email.com");
        callAcceptRequestDto.setPassengerEmail("passenger@email.com");

        RideInfoDto rideInfoDto = new RideInfoDto();

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

        given(rideService.acceptCall(any())).willReturn(rideInfoDto);
        willDoNothing().given(rideProducer).sendRideInfo(rideInfoDto);

        mockMvc.perform(post("/api/ride/accept")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(callAcceptRequestDto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void 택시_호출_수락_valid() throws Exception {
        CallAcceptRequestDto callAcceptRequestDto = new CallAcceptRequestDto();
        callAcceptRequestDto.setDriverEmail("driver@email.com");

        mockMvc.perform(post("/api/ride/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(callAcceptRequestDto)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    void 택시_호출_취소_테스트() throws Exception {
        willDoNothing().given(rideService).cancelRide(any());

        mockMvc.perform(post("/api/ride/cancel/{rideId}", 1L))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void 택시_시작_테스트() throws Exception {
        willDoNothing().given(rideService).startRide(any());

        mockMvc.perform(post("/api/ride/start/{rideId}", 1L))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    void 택시_완료_테스트() throws Exception {
        RideCompleteDto dto = new RideCompleteDto();

        dto.setRideId(5L);
        dto.setFare(50000);

        willDoNothing().given(rideService).completeRide(dto);

        mockMvc.perform(post("/api/ride/complete")
                        .content(new ObjectMapper().writeValueAsString(dto))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }
}