package com.first.gateway.repository;
import com.first.gateway.domain.entity.McpServer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface McpServerRepository extends JpaRepository<McpServer, Long> {
    List<McpServer> findByUserIdAndDeletedOrderByUpdatedAtDesc(Long userId, Short deleted);
    Optional<McpServer> findByIdAndUserIdAndDeleted(Long id, Long userId, Short deleted);
}