package com.first.gateway.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final ApplicationEventPublisher eventPublisher;

    public NotificationService(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishStatsUpdate(Long userId, long requests, long tokens, double cost) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requests", requests);
        payload.put("tokens", tokens);
        payload.put("cost", cost);
        publish(userId, "STATS_UPDATE", payload);
    }

    public void publishMemoryExtracted(Long userId, int count, List<String> categories) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("count", count);
        payload.put("categories", categories);
        publish(userId, "MEMORY_EXTRACTED", payload);
    }

    public void publishProfileUpdated(Long userId, String summaryPreview) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary_preview", summaryPreview);
        publish(userId, "PROFILE_UPDATED", payload);
    }

    public void publishScheduleReminder(Long userId, Long memoryId, String content, String time) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("memory_id", memoryId);
        payload.put("content", content);
        payload.put("time", time);
        publish(userId, "SCHEDULE_REMINDER", payload);
    }

    public void publishDocIndexDone(Long userId, Long docId, Long kbId, String filename) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("doc_id", docId);
        payload.put("kb_id", kbId);
        payload.put("filename", filename);
        publish(userId, "DOC_INDEX_DONE", payload);
    }

    private void publish(Long userId, String type, Map<String, Object> payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        event.put("payload", payload);
        event.put("timestamp", Instant.now().toString());

        log.debug("Publishing notification for user {}: {}", userId, type);
        eventPublisher.publishEvent(new UserNotificationEvent(userId, event));
    }
}
