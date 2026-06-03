package com.first.gateway.service.profile;

import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.UserMemoryRepository;
import com.first.gateway.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class UserMemoryService {

    private final UserMemoryRepository memoryRepository;
    private final UserProfileRepository profileRepository;

    public UserMemoryService(UserMemoryRepository memoryRepository, UserProfileRepository profileRepository) {
        this.memoryRepository = memoryRepository;
        this.profileRepository = profileRepository;
    }

    public List<UserMemory> list(Long userId, String category, String status) {
        String effectiveStatus = status != null && !status.isBlank() ? status : "ACTIVE";
        if (category != null && !category.isBlank()) {
            return memoryRepository.findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(
                userId, category, effectiveStatus);
        }
        return memoryRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, effectiveStatus);
    }

    public List<Map<String, Object>> todaySchedule(Long userId) {
        LocalDate today = LocalDate.now();
        return memoryRepository.findByUserIdAndScheduleDateAndStatus(userId, today, "ACTIVE").stream()
            .filter(m -> "SCHEDULE".equals(m.getCategory()) || "TODO".equals(m.getCategory()))
            .map(this::toScheduleEntry)
            .toList();
    }

    public List<Map<String, Object>> upcomingSchedule(Long userId) {
        LocalDate today = LocalDate.now();
        List<UserMemory> rows = memoryRepository.findUpcomingSchedulesByUser(userId, today);
        if (rows.isEmpty()) {
            rows = memoryRepository.findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(
                userId, "SCHEDULE", "ACTIVE");
            if (rows.isEmpty()) {
                rows = memoryRepository.findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(
                    userId, "TODO", "ACTIVE");
            }
        }
        return rows.stream()
            .limit(10)
            .map(this::toScheduleEntry)
            .toList();
    }

    public UserMemory require(Long id, Long userId) {
        return memoryRepository.findById(id)
            .filter(m -> userId.equals(m.getUserId()))
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "memory not found"));
    }

    @Transactional
    public UserMemory create(Long userId, Long tenantId, String category, String content,
                             Short importance, LocalDate scheduleDate, String scheduleTime,
                             BigDecimal numericValue, Long conversationId) {
        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setTenantId(tenantId);
        memory.setConversationId(conversationId);
        memory.setSource("MANUAL");
        memory.setCategory(requireCategory(category));
        memory.setContent(requireContent(content));
        memory.setImportance(importance != null ? importance : (short) 3);
        memory.setScheduleDate(scheduleDate);
        memory.setScheduleTime(scheduleTime);
        memory.setNumericValue(numericValue);
        memory.setStatus("ACTIVE");
        UserMemory saved = memoryRepository.save(memory);
        syncMemoryCount(userId);
        return saved;
    }

    @Transactional
    public UserMemory update(Long id, Long userId, String content, Short importance,
                             LocalDate scheduleDate, String scheduleTime, BigDecimal numericValue) {
        UserMemory memory = require(id, userId);
        if (content != null && !content.isBlank()) {
            memory.setContent(content.trim());
        }
        if (importance != null) {
            memory.setImportance(importance);
        }
        if (scheduleDate != null) {
            memory.setScheduleDate(scheduleDate);
        }
        if (scheduleTime != null) {
            memory.setScheduleTime(scheduleTime);
        }
        if (numericValue != null) {
            memory.setNumericValue(numericValue);
        }
        return memoryRepository.save(memory);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        UserMemory memory = require(id, userId);
        memoryRepository.delete(memory);
        syncMemoryCount(userId);
    }

    @Transactional
    public UserMemory markDone(Long id, Long userId) {
        UserMemory memory = require(id, userId);
        memory.setStatus("DONE");
        UserMemory saved = memoryRepository.save(memory);
        syncMemoryCount(userId);
        return saved;
    }

    @Transactional
    public UserMemory archive(Long id, Long userId) {
        UserMemory memory = require(id, userId);
        memory.setStatus("ARCHIVED");
        UserMemory saved = memoryRepository.save(memory);
        syncMemoryCount(userId);
        return saved;
    }

    @Transactional
    public void deleteAllForUser(Long userId) {
        for (UserMemory memory : memoryRepository.findByUserId(userId)) {
            memoryRepository.delete(memory);
        }
        syncMemoryCount(userId);
    }

    private void syncMemoryCount(Long userId) {
        profileRepository.findByUserId(userId).ifPresent(profile -> {
            int count = (int) memoryRepository.countByUserIdAndStatus(userId, "ACTIVE");
            profile.setMemoryCount(count);
            profileRepository.save(profile);
        });
    }

    private Map<String, Object> toScheduleEntry(UserMemory memory) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", memory.getId());
        entry.put("date", memory.getScheduleDate() != null ? memory.getScheduleDate().toString() : null);
        entry.put("time", memory.getScheduleTime());
        entry.put("content", memory.getContent());
        entry.put("status", memory.getStatus());
        entry.put("category", memory.getCategory());
        return entry;
    }

    private static String requireCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "category is required");
        }
        return category.trim().toUpperCase();
    }

    public List<UserMemory> listForUser(Long userId, String category) {
        if (category != null && !category.isBlank()) {
            return memoryRepository.findByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(userId, category, "ACTIVE");
        }
        return memoryRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE");
    }

    @Transactional
    public UserMemory createFromExtraction(UserMemory memory) {
        return memoryRepository.save(memory);
    }

    public long countForUser(Long userId) {
        return memoryRepository.countByUserIdAndStatus(userId, "ACTIVE");
    }

    public List<UserMemory> recentEvents(Long userId, int limit) {
        return memoryRepository.findByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(userId, "EVENT", "ACTIVE")
            .stream().limit(limit).toList();
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "content is required");
        }
        return content.trim();
    }
}