package com.first.gateway.web.workspace.dto;

public record KnowledgeSearchResult(Long documentId, String title, long score, String snippet) {}
