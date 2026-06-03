package com.first.gateway.service.scheduler;

import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.repository.UserMemoryRepository;
import com.first.gateway.service.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private final UserMemoryRepository memoryRepository;
    private final NotificationService notificationService;

    public ScheduledTasks(UserMemoryRepository memoryRepository,
                           NotificationService notificationService) {
        this.memoryRepository = memoryRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void checkScheduleReminders() {
        LocalDate today = LocalDate.now();
        String nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String futureTime = LocalTime.now().plusMinutes(15).format(DateTimeFormatter.ofPattern("HH:mm"));

        List<UserMemory> upcoming = memoryRepository.findUpcomingSchedules(today, nowTime, futureTime);
        for (UserMemory m : upcoming) {
            if (m.getReminded() != null && m.getReminded() == 1) continue;
            m.setReminded((short) 1);
            memoryRepository.save(m);
            notificationService.publishScheduleReminder(
                m.getUserId(), m.getId(), m.getContent(), m.getScheduleTime());
            log.info("Schedule reminder sent for memory {} to user {}", m.getId(), m.getUserId());
        }
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void archiveExpiredSchedules() {
        LocalDate today = LocalDate.now();
        int archived = memoryRepository.archiveExpiredSchedules(today);
        if (archived > 0) {
            log.info("Archived {} expired schedules", archived);
        }
    }

    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupOldData() {
        Instant cutoff = Instant.now().minus(Duration.ofDays(90));
        int deleted = memoryRepository.cleanupOldData(cutoff);
        if (deleted > 0) {
            log.info("Cleaned up {} old memory records", deleted);
        }
    }
}
