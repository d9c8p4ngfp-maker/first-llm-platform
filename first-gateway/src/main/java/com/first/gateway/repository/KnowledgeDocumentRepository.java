package com.first.gateway.repository;

import com.first.gateway.domain.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {

    List<KnowledgeDocument> findByKnowledgeBaseIdAndDeletedOrderByUpdatedAtDesc(Long knowledgeBaseId, Short deleted);

    long countByKnowledgeBaseIdAndDeleted(Long knowledgeBaseId, Short deleted);
}