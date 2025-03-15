package com.ogeedeveloper.backend.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSockerController {
    private SimpMessagingTemplate messagingTemplate;

//    Function to send notification to client
    public void sendNotification(String message) {
        messagingTemplate.convertAndSend("/topic/notification", message);
    }
}
