package com.first.gateway.service.profile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.domain.entity.UserProfile;
import com.first.gateway.domain.enums.SynthesisStatus;
import com.first.gateway.repository.UserProfileRepository;
import com.first.gateway.service.profile.ProfileSynthesisService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;
    private final ProfileSynthesisService synthesisService;
    private final UserProfileService self;

    public UserProfileService(UserProfileRepository profileRepository,
                              UserMemoryService userMemoryService,
                              ObjectMapper objectMapper,
                              @Lazy ProfileSynthesisService synthesisService,
                              @Lazy UserProfileService self) {
        this.profileRepository = profileRepository;
        this.userMemoryService = userMemoryService;
        this.objectMapper = objectMapper;
        this.synthesisService = synthesisService;
        this.self = self;
    }

    @Transactional(readOnly = false)
    public UserProfile getOrCreate(Long userId, Long tenantId) {
        return profileRepository.findByUserId(userId).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUserId(userId);
            profile.setTenantId(tenantId);
            profile.setMemoryCount(0);
            profile.setProfileEnabled((short) 1);
            return profileRepository.save(profile);
        });
    }

    @Transactional(readOnly = false)
    public Map<String, Object> getProfile(Long userId, Long tenantId, String username) {
        UserProfile profile = self.getOrCreate(userId, tenantId);
        Map<String, Object> body = new LinkedHashMap<>();
        String nickname = profile.getNickname();
        if (nickname == null || nickname.isBlank()) {
            nickname = username != null ? username : "User";
        }
        body.put("nickname", nickname);
        body.put("mbti", profile.getMbti());
        body.put("mbti_label", profile.getMbtiLabel());
        body.put("zodiac", profile.getZodiac());
        body.put("primary_tag", profile.getPrimaryTag());
        body.put("tags", parseTags(profile.getAiTags()));
        body.put("ai_summary", profile.getAiSummary());
        body.put("memory_count", profile.getMemoryCount() != null ? profile.getMemoryCount() : 0);
        body.put("schedule_count", todayScheduleCount(userId));
        body.put("profile_ready", isProfileReady(profile));
        return body;
    }

    @Transactional(readOnly = false)
    public Map<String, Object> profileSummary(Long userId, Long tenantId, String username) {
        Map<String, Object> full = getProfile(userId, tenantId, username);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("nickname", full.get("nickname"));
        summary.put("mbti", full.get("mbti"));
        summary.put("mbti_label", full.get("mbti_label"));
        summary.put("zodiac", full.get("zodiac"));
        summary.put("primary_tag", full.get("primary_tag"));
        return summary;
    }

    @Transactional
    public Map<String, Object> refresh(Long userId, Long tenantId) {
        UserProfile profile = self.getOrCreate(userId, tenantId);
        if (profile.getSynthesisStatus() == SynthesisStatus.RUNNING
            || profile.getSynthesisStatus() == SynthesisStatus.PENDING) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("status", "running");
            body.put("message", "profile synthesis is already in progress");
            return body;
        }
        synthesisService.checkAndTrigger(userId, tenantId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "queued");
        body.put("message", "profile synthesis started");
        return body;
    }

    @Transactional
    public void updateMemoryCount(Long userId, Long tenantId) {
        UserProfile profile = self.getOrCreate(userId, tenantId);
        int count = (int) userMemoryService.countForUser(userId);
        profile.setMemoryCount(count);
        profileRepository.save(profile);
    }

    @Transactional
    public void clearAll(Long userId, Long tenantId) {
        userMemoryService.deleteAllForUser(userId);
        profileRepository.findByUserId(userId).ifPresent(profileRepository::delete);
        self.getOrCreate(userId, tenantId);
    }

    private boolean isProfileReady(UserProfile profile) {
        return (profile.getAiSummary() != null && !profile.getAiSummary().isBlank())
            || (profile.getMemoryCount() != null && profile.getMemoryCount() > 0);
    }

    private int todayScheduleCount(Long userId) {
        return userMemoryService.todaySchedule(userId).size();
    }

    private List<String> parseTags(String aiTags) {
        if (aiTags == null || aiTags.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(aiTags, new TypeReference<List<String>>() {});
        } catch (Exception ignored) {
            return List.of(aiTags.trim());
        }
    }
}