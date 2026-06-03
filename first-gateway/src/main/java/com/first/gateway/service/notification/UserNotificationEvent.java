package com.first.gateway.service.notification;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

public class UserNotificationEvent extends ApplicationEvent {

    private final Long userId;
    private final Map<String, Object> event;

    public UserNotificationEvent(Long userId, Map<String, Object> event) {
        super(event);
        this.userId = userId;
        this.event = event;
    }

    public Long getUserId() { return userId; }
    public Map<String, Object> getEvent() { return event; }
}
