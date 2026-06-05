package com.first.gateway.domain.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MemoryCategoryTest {

    @Test
    void shouldContainFourCategories() {
        assertThat(MemoryCategory.values())
            .containsExactly(MemoryCategory.FACT, MemoryCategory.SCHEDULE,
                MemoryCategory.TODO, MemoryCategory.EVENT);
    }
}
