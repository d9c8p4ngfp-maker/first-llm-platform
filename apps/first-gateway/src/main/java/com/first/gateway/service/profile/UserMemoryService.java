package com.first.gateway.service.profile;

import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.enums.MemoryCategory;
import com.first.gateway.domain.enums.MemoryStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.UserMemoryRepository;
import com.first.gateway.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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
        MemoryStatus effectiveStatus = status != null && !status.isBlank()
            ? MemoryStatus.valueOf(status.toUpperCase())
            : MemoryStatus.ACTIVE;
        if (category != null && !category.isBlank()) {
            MemoryCategory cat = MemoryCategory.valueOf(category.toUpperCase());
            return memoryRepository.findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(
                userId, cat, effectiveStatus);
        }
        return memoryRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, effectiveStatus);
    }

    public List<Map<String, Object>> todaySchedule(Long userId) {
        LocalDate today = LocalDate.now();
        return memoryRepository.findByUserIdAndScheduleDateAndStatus(userId, today, MemoryStatus.ACTIVE).stream()
            .filter(m -> MemoryCategory.SCHEDULE.equals(m.getCategory()) || MemoryCategory.TODO.equals(m.getCategory()))
            .map(this::toScheduleEntry)
            .toList();
    }

    public List<Map<String, Object>> upcomingSchedule(Long userId) {
        LocalDate today = LocalDate.now();
        List<UserMemory> rows = memoryRepository.findUpcomingSchedulesByUser(userId, today,
            MemoryStatus.ACTIVE, List.of(MemoryCategory.SCHEDULE, MemoryCategory.TODO));
        if (rows.isEmpty()) {
            rows = memoryRepository.findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(
                userId, MemoryCategory.SCHEDULE, MemoryStatus.ACTIVE);
            if (rows.isEmpty()) {
                rows = memoryRepository.findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(
                    userId, MemoryCategory.TODO, MemoryStatus.ACTIVE);
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
        memory.setStatus(MemoryStatus.ACTIVE);
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
        memory.setStatus(MemoryStatus.DONE);
        UserMemory saved = memoryRepository.save(memory);
        syncMemoryCount(userId);
        return saved;
    }

    @Transactional
    public UserMemory archive(Long id, Long userId) {
        UserMemory memory = require(id, userId);
        memory.setStatus(MemoryStatus.ARCHIVED);
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
            int count = (int) memoryRepository.countByUserIdAndStatus(userId, MemoryStatus.ACTIVE);
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

    private static MemoryCategory requireCategory(String category) {
        if (category == null || category.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "category is required");
        }
        try {
            return MemoryCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "invalid category: " + category);
        }
    }

    public List<UserMemory> listRelevantForChat(Long userId, String query, int maxResults) {
        if (query == null || query.isBlank() || maxResults <= 0) {
            return List.of();
        }
        List<UserMemory> all = memoryRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, MemoryStatus.ACTIVE);
        if (all.isEmpty()) {
            return List.of();
        }
        String normalizedQuery = query.trim().toLowerCase();
        return all.stream()
            .map(memory -> Map.entry(memory, scoreMemoryRelevance(memory, normalizedQuery)))
            .filter(entry -> entry.getValue() >= MIN_RELEVANCE_SCORE)
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(maxResults)
            .map(Map.Entry::getKey)
            .toList();
    }

    private static final double MIN_RELEVANCE_SCORE = 2.0;

    private static double scoreMemoryRelevance(UserMemory memory, String query) {
        String content = memory.getContent();
        if (content == null || content.isBlank()) {
            return 0;
        }
        String normalizedContent = content.trim().toLowerCase();
        double score = 0;

        if (query.contains(normalizedContent) || normalizedContent.contains(query)) {
            score += 6;
        }

        for (String token : tokenize(query)) {
            if (token.length() >= 2 && normalizedContent.contains(token)) {
                score += token.length() >= 3 ? 2 : 1;
            }
        }

        MemoryCategory category = memory.getCategory();
        if ((category == MemoryCategory.SCHEDULE || category == MemoryCategory.TODO)
            && matchesScheduleIntent(query)) {
            score += 2;
        }
        if (category == MemoryCategory.FACT && matchesIdentityIntent(query)) {
            score += 2;
        }

        return score;
    }

    private static boolean matchesScheduleIntent(String query) {
        return query.matches(".*(今天|明天|后天|大后天|日程|安排|计划|几点|什么时候|待办|记得|行程|约会).*");
    }

    private static boolean matchesIdentityIntent(String query) {
        return query.matches(".*(我叫|名字|是谁|称呼|叫我|姓名|介绍).*");
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        String[] parts = text.split("\\s+");
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            tokens.add(part);
            if (part.codePointCount(0, part.length()) > 1) {
                int[] cps = part.codePoints().toArray();
                for (int cp : cps) {
                    tokens.add(new String(Character.toChars(cp)));
                }
                for (int i = 0; i < cps.length - 1; i++) {
                    tokens.add(new String(Character.toChars(cps[i]))
                        + new String(Character.toChars(cps[i + 1])));
                }
            }
        }
        return tokens;
    }

    public List<UserMemory> listForUser(Long userId, String category) {
        MemoryStatus activeStatus = MemoryStatus.ACTIVE;
        if (category != null && !category.isBlank()) {
            MemoryCategory cat = MemoryCategory.valueOf(category.toUpperCase());
            return memoryRepository.findByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(userId, cat, activeStatus);
        }
        return memoryRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, activeStatus);
    }

    @Transactional
    public UserMemory createFromExtraction(UserMemory memory) {
        return memoryRepository.save(memory);
    }

    public long countForUser(Long userId) {
        return memoryRepository.countByUserIdAndStatus(userId, MemoryStatus.ACTIVE);
    }

    public List<UserMemory> recentEvents(Long userId, int limit) {
        return memoryRepository.findByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(userId, MemoryCategory.EVENT, MemoryStatus.ACTIVE)
            .stream().limit(limit).toList();
    }

    private static String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "content is required");
        }
        return content.trim();
    }
}
