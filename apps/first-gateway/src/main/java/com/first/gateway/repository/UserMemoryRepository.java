package com.first.gateway.repository;

import com.first.gateway.domain.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface UserMemoryRepository extends JpaRepository<UserMemory, Long> {
    List<UserMemory> findByUserId(Long userId);
    List<UserMemory> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);
    List<UserMemory> findByUserIdAndCategoryAndStatusOrderByUpdatedAtDesc(Long userId, String category, String status);
    List<UserMemory> findByUserIdAndScheduleDateAndStatus(Long userId, LocalDate scheduleDate, String status);
    long countByUserIdAndStatus(Long userId, String status);

    @Query("""
        SELECT m FROM UserMemory m
        WHERE m.userId = :userId
          AND m.status = 'ACTIVE'
          AND m.category IN ('SCHEDULE', 'TODO')
          AND (m.scheduleDate >= :fromDate OR m.scheduleDate IS NULL)
        ORDER BY m.scheduleDate ASC, m.scheduleTime ASC
        """)
    List<UserMemory> findUpcomingSchedulesByUser(@Param("userId") Long userId,
                                                 @Param("fromDate") LocalDate fromDate);
    List<UserMemory> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
    List<UserMemory> findByUserIdAndCategoryAndStatusOrderByCreatedAtDesc(Long userId, String category, String status);

    @Query("SELECT m FROM UserMemory m WHERE m.category = 'SCHEDULE' AND m.scheduleDate = :today " +
           "AND m.scheduleTime >= :fromTime AND m.scheduleTime <= :toTime " +
           "AND m.status = 'ACTIVE' AND (m.reminded IS NULL OR m.reminded = 0)")
    List<UserMemory> findUpcomingSchedules(@Param("today") LocalDate today,
                                           @Param("fromTime") String fromTime,
                                           @Param("toTime") String toTime);

    @Modifying
    @Query("UPDATE UserMemory m SET m.status = 'ARCHIVED' WHERE m.category = 'SCHEDULE' " +
           "AND m.scheduleDate < :today AND m.status = 'ACTIVE'")
    int archiveExpiredSchedules(@Param("today") LocalDate today);

    @Modifying
    @Query("DELETE FROM UserMemory m WHERE m.status IN ('DELETED', 'ARCHIVED') AND m.updatedAt < :cutoff")
    int cleanupOldData(@Param("cutoff") Instant cutoff);
}
