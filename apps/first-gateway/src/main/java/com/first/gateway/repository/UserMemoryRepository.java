package com.first.gateway.repository;

import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.enums.MemoryCategory;
import com.first.gateway.domain.enums.MemoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
    List<UserMemory> findByUserId(Long userId);
    List<UserMemory> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, MemoryStatus status);
    List<UserMemory> findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(Long userId, MemoryCategory category, MemoryStatus status);
    List<UserMemory> findByUserIdAndScheduleDateAndStatus(Long userId, LocalDate scheduleDate, MemoryStatus status);
    long countByUserIdAndStatus(Long userId, MemoryStatus status);

    @Query("""
        SELECT m FROM UserMemory m
        WHERE m.userId = :userId
          AND m.status = :status
          AND m.category IN :categories
          AND (m.scheduleDate >= :fromDate OR m.scheduleDate IS NULL)
        ORDER BY m.scheduleDate ASC, m.scheduleTime ASC
        """)
    List<UserMemory> findUpcomingSchedulesByUser(@Param("userId") Long userId,
                                                 @Param("fromDate") LocalDate fromDate,
                                                 @Param("status") MemoryStatus status,
                                                 @Param("categories") List<MemoryCategory> categories);
    List<UserMemory> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, MemoryStatus status);
    List<UserMemory> findByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(Long userId, MemoryCategory category, MemoryStatus status);

    @Query("SELECT m FROM UserMemory m WHERE m.category = :category AND m.scheduleDate = :today " +
           "AND m.scheduleTime >= :fromTime AND m.scheduleTime <= :toTime " +
           "AND m.status = :status AND (m.reminded IS NULL OR m.reminded = 0)")
    List<UserMemory> findUpcomingSchedules(@Param("today") LocalDate today,
                                           @Param("fromTime") String fromTime,
                                           @Param("toTime") String toTime,
                                           @Param("category") MemoryCategory category,
                                           @Param("status") MemoryStatus status);

    @Modifying
    @Query("UPDATE UserMemory m SET m.status = :archivedStatus WHERE m.category = :category " +
           "AND m.scheduleDate < :today AND m.status = :activeStatus")
    int archiveExpiredSchedules(@Param("today") LocalDate today,
                                @Param("archivedStatus") MemoryStatus archivedStatus,
                                @Param("category") MemoryCategory category,
                                @Param("activeStatus") MemoryStatus activeStatus);

    @Modifying
    @Query("DELETE FROM UserMemory m WHERE m.status IN :statuses AND m.updatedAt < :cutoff")
    int cleanupOldData(@Param("cutoff") Instant cutoff,
                       @Param("statuses") List<MemoryStatus> statuses);
}
