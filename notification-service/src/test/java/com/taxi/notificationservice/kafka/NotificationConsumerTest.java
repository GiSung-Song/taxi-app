package com.taxi.notificationservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxi.common.core.dto.DriveCompleteDto;
import com.taxi.common.core.dto.RideAcceptDto;
import com.taxi.common.core.dto.RideCancelDto;
import com.taxi.common.core.dto.RideStartDto;
import com.taxi.common.security.JwtTokenUtil;
import com.taxi.notificationservice.config.KafkaContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(KafkaContainerConfig.class)
class NotificationConsumerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private WebSocketStompClient stompClient;
    private BlockingQueue<String> passengerMessages;
    private BlockingQueue<String> driverMessages;

    private String passengerEmail = "passenger@email.com";
    private String driverEmail = "driver@email.com";

    private String passengerToken;
    private String driverToken;

    @BeforeEach
    void setUp() throws Exception {
        passengerMessages = new LinkedBlockingQueue<>();
        driverMessages    = new LinkedBlockingQueue<>();

        passengerToken = "Bearer " + jwtTokenUtil.generateAccessToken(passengerEmail, "USER");
        driverToken    = "Bearer " + jwtTokenUtil.generateAccessToken(driverEmail, "DRIVER");

        SockJsClient sockJsClient = new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())));
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new StringMessageConverter());

        connectUser(passengerToken, passengerMessages);
        connectUser(driverToken, driverMessages);
    }

    private void connectUser(String token, BlockingQueue<String> messageQueue) throws Exception {
        String url = "ws://localhost:" + port + "/ws";

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, token);

        StompSessionHandlerAdapter handlerAdapter = new StompSessionHandlerAdapter() { };
        StompSession session = stompClient.connectAsync(url, headers, handlerAdapter).get(3, TimeUnit.SECONDS);

        session.subscribe("/user/queue/notification", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                messageQueue.add((String) payload);
            }
        });
    }

    @Test
    void 호출_수락_consumer_test() throws JsonProcessingException, InterruptedException {
        RideAcceptDto rideAcceptDto = new RideAcceptDto();

        rideAcceptDto.setRideId(0L);
        rideAcceptDto.setPassengerUserId(123L);
        rideAcceptDto.setPassengerPhoneNumber("01012341234");
        rideAcceptDto.setStartLocation("출발지");
        rideAcceptDto.setEndLocation("도착지");
        rideAcceptDto.setRideStatus("ACCEPT");
        rideAcceptDto.setAcceptTime(LocalDateTime.now());

        rideAcceptDto.setDriverUserId(456L);
        rideAcceptDto.setDriverName("기사");
        rideAcceptDto.setDriverPhoneNumber("01023452345");
        rideAcceptDto.setCarName("코란도");
        rideAcceptDto.setCarNumber("123가1234");
        rideAcceptDto.setCapacity(4);
        rideAcceptDto.setTotalRides(50);

        rideAcceptDto.setPassengerEmail(passengerEmail);
        rideAcceptDto.setDriverEmail(driverEmail);

        String message = objectMapper.writeValueAsString(rideAcceptDto);

        kafkaTemplate.send("ride-accept", message);

        String passengerMessage = passengerMessages.poll(5, TimeUnit.SECONDS);
        String driverMessage = driverMessages.poll(5, TimeUnit.SECONDS);

        System.out.println("passengerMessage = " + passengerMessage);
        System.out.println("driverMessage = " + driverMessage);

        assertNotNull(passengerMessage, "승객용 메시지 받음");
        assertTrue(passengerMessage.contains("출발지"));
        assertTrue(passengerMessage.contains("도착지"));
        assertTrue(passengerMessage.contains("01023452345"));
        assertTrue(passengerMessage.contains("코란도"));
        assertTrue(passengerMessage.contains("기사"));
        assertFalse(passengerMessage.contains("01012341234"));

        assertNotNull(driverMessage, "기사용 메시지 받음");
        assertTrue(driverMessage.contains("출발지"));
        assertTrue(driverMessage.contains("도착지"));
        assertTrue(driverMessage.contains("01012341234"));
        assertFalse(driverMessage.contains("01023452345"));
    }

    @Test
    void 호출_취소_consumer_test() throws JsonProcessingException, InterruptedException {
        RideCancelDto rideCancelDto = new RideCancelDto();

        rideCancelDto.setRideId(0L);
        rideCancelDto.setPassengerUserId(123L);
        rideCancelDto.setRideStatus("CANCEL");
        rideCancelDto.setCancelTime(LocalDateTime.now());
        rideCancelDto.setDriverUserId(456L);
        rideCancelDto.setPassengerEmail(passengerEmail);
        rideCancelDto.setDriverEmail(driverEmail);

        String message = objectMapper.writeValueAsString(rideCancelDto);

        kafkaTemplate.send("ride-cancel", message);

        String passengerMessage = passengerMessages.poll(5, TimeUnit.SECONDS);
        String driverMessage = driverMessages.poll(5, TimeUnit.SECONDS);

        System.out.println("passengerMessage = " + passengerMessage);
        System.out.println("driverMessage = " + driverMessage);

        assertNotNull(passengerMessage, "승객용 메시지 받음");
        assertNotNull(driverMessage, "기사용 메시지 받음");
    }

    @Test
    void 운행_시작_consumer_test() throws JsonProcessingException, InterruptedException {
        RideStartDto rideStartDto = new RideStartDto();

        rideStartDto.setRideId(0L);
        rideStartDto.setPassengerUserId(123L);
        rideStartDto.setPassengerPhoneNumber("01012341234");
        rideStartDto.setStartLocation("출발지");
        rideStartDto.setEndLocation("도착지");
        rideStartDto.setRideStatus("START");
        rideStartDto.setStartTime(LocalDateTime.now());

        rideStartDto.setDriverUserId(456L);
        rideStartDto.setDriverName("기사");
        rideStartDto.setDriverPhoneNumber("01023452345");
        rideStartDto.setCarName("코란도");
        rideStartDto.setCarNumber("123가1234");

        rideStartDto.setPassengerEmail(passengerEmail);
        rideStartDto.setDriverEmail(driverEmail);

        String message = objectMapper.writeValueAsString(rideStartDto);

        kafkaTemplate.send("ride-start", message);

        String passengerMessage = passengerMessages.poll(5, TimeUnit.SECONDS);
        String driverMessage = driverMessages.poll(5, TimeUnit.SECONDS);

        System.out.println("passengerMessage = " + passengerMessage);
        System.out.println("driverMessage = " + driverMessage);

        assertNotNull(passengerMessage, "승객용 메시지 받음");
        assertTrue(passengerMessage.contains("출발지"));
        assertTrue(passengerMessage.contains("도착지"));
        assertTrue(passengerMessage.contains("01023452345"));
        assertTrue(passengerMessage.contains("코란도"));
        assertTrue(passengerMessage.contains("기사"));
        assertFalse(passengerMessage.contains("01012341234"));

        assertNotNull(driverMessage, "기사용 메시지 받음");
        assertTrue(driverMessage.contains("출발지"));
        assertTrue(driverMessage.contains("도착지"));
        assertTrue(driverMessage.contains("01012341234"));
        assertFalse(driverMessage.contains("01023452345"));
    }

    @Test
    void 운행_종료_consumer_test() throws JsonProcessingException, InterruptedException {
        DriveCompleteDto driveCompleteDto = new DriveCompleteDto();

        driveCompleteDto.setRideId(0L);
        driveCompleteDto.setPassengerUserId(123L);
        driveCompleteDto.setPassengerPhoneNumber("01012341234");
        driveCompleteDto.setStartLocation("출발지");
        driveCompleteDto.setEndLocation("도착지");
        driveCompleteDto.setRideStatus("ACCEPT");
        driveCompleteDto.setCompleteTime(LocalDateTime.now());

        driveCompleteDto.setDriverUserId(456L);
        driveCompleteDto.setDriverName("기사");
        driveCompleteDto.setDriverPhoneNumber("01023452345");
        driveCompleteDto.setCarName("코란도");
        driveCompleteDto.setCarNumber("123가1234");
        driveCompleteDto.setFare(50000);

        driveCompleteDto.setPassengerEmail(passengerEmail);
        driveCompleteDto.setDriverEmail(driverEmail);

        String message = objectMapper.writeValueAsString(driveCompleteDto);

        kafkaTemplate.send("ride-complete", message);

        String passengerMessage = passengerMessages.poll(5, TimeUnit.SECONDS);
        String driverMessage = driverMessages.poll(5, TimeUnit.SECONDS);

        System.out.println("passengerMessage = " + passengerMessage);
        System.out.println("driverMessage = " + driverMessage);

        assertNotNull(passengerMessage, "승객용 메시지 받음");
        assertTrue(passengerMessage.contains("출발지"));
        assertTrue(passengerMessage.contains("도착지"));
        assertTrue(passengerMessage.contains("01023452345"));
        assertTrue(passengerMessage.contains("코란도"));
        assertTrue(passengerMessage.contains("기사"));
        assertTrue(passengerMessage.contains("50000"));
        assertFalse(passengerMessage.contains("01012341234"));

        assertNotNull(driverMessage, "기사용 메시지 받음");
        assertTrue(driverMessage.contains("출발지"));
        assertTrue(driverMessage.contains("도착지"));
        assertTrue(driverMessage.contains("01012341234"));
        assertTrue(driverMessage.contains("50000"));
        assertFalse(driverMessage.contains("01023452345"));
    }
}