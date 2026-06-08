package com.first.gateway.service.profile;

import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.enums.MemoryCategory;
import com.first.gateway.domain.enums.MemoryStatus;
import com.first.gateway.repository.UserMemoryRepository;
import com.first.gateway.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMemoryServiceRelevanceTest {

    @Mock private UserMemoryRepository memoryRepository;
    @Mock private UserProfileRepository profileRepository;

    private UserMemoryService service;

    @BeforeEach
    void setUp() {
        service = new UserMemoryService(memoryRepository, profileRepository);
    }

    @Test
    void listRelevantForChat_shouldMatchScheduleMemoriesForScheduleQuery() {
        UserMemory schedule = memory(2L, MemoryCategory.SCHEDULE, "后天去飞天");
        UserMemory unrelated = memory(3L, MemoryCategory.FACT, "周末去公园打篮球");
        when(memoryRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, MemoryStatus.ACTIVE))
            .thenReturn(List.of(unrelated, schedule));

        List<UserMemory> result = service.listRelevantForChat(1L, "后天我会飞天", 5);

        assertThat(result).extracting(UserMemory::getId).containsExactly(2L);
    }

    @Test
    void listRelevantForChat_shouldSkipMemoriesForUnrelatedDatabaseQuery() {
        UserMemory schedule = memory(2L, MemoryCategory.SCHEDULE, "后天去飞天");
        UserMemory fact = memory(1L, MemoryCategory.FACT, "用户名叫孙建峰");
        when(memoryRepository.findByUserIdAndStatusOrderByCreatedAtDesc(1L, MemoryStatus.ACTIVE))
            .thenReturn(List.of(schedule, fact));

        List<UserMemory> result = service.listRelevantForChat(1L, "我数据库有啥", 5);

        assertThat(result).isEmpty();
    }

    private static UserMemory memory(Long id, MemoryCategory category, String content) {
        UserMemory memory = new UserMemory();
        memory.setId(id);
        memory.setCategory(category);
        memory.setContent(content);
        memory.setStatus(MemoryStatus.ACTIVE);
        return memory;
    }
}
