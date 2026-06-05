package com.first.gateway.web.workspace.dto;

public record UserProfileResponse(String nickname, String mbti, String mbtiLabel, String zodiac, String aiSummary, String aiTags, String aiSystemPrompt, String synthesisStatus) {}
