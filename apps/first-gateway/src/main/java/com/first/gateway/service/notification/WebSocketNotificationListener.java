package com.first.gateway.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketNotificationListener.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketNotificationListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleNotification(UserNotificationEvent event) {
        try {
            String destination = "/queue/notifications";
            messagingTemplate.convertAndSendToUser(
                event.getUserId().toString(),
                destination,
                event.getEvent()
            );
            log.debug("Sent WebSocket notification to user {}: {}", event.getUserId(), event.getEvent().get("type"));
        } catch (Exception e) {
            log.warn("Failed to send WebSocket notification: {}", e.getMessage());
        }
    }
}
