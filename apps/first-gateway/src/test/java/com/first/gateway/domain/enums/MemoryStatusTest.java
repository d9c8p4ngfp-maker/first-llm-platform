package com.first.gateway.domain.enums;

import com.first.gateway.domain.entity.UserMemory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MemoryStatusTest {

    @Test
    void shouldContainFourStatuses() {
        assertThat(MemoryStatus.values())
            .containsExactly(MemoryStatus.ACTIVE, MemoryStatus.ARCHIVED,
                MemoryStatus.DELETED, MemoryStatus.DONE);
    }

    @Test
    void userMemoryDefaultStatusIsActive() {
        UserMemory m = new UserMemory();
        assertThat(m.getStatus()).isEqualTo(MemoryStatus.ACTIVE);
    }
}
