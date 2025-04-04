package com.taxi.notificationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @PostMapping("/send")
    public ResponseEntity<Void> sendTestMessage(@RequestParam String userId) {
        simpMessagingTemplate.convertAndSendToUser(userId, "/queue/notification", "테스트 메시지 입니다.");

        return ResponseEntity.ok().build();
    }

}