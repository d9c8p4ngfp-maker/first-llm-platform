package com.first.gateway.domain.enums;

import com.first.gateway.domain.entity.UserProfile;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SynthesisStatusTest {

    @Test
    void shouldContainFourStatuses() {
        assertThat(SynthesisStatus.values())
            .containsExactly(SynthesisStatus.IDLE, SynthesisStatus.PENDING,
                SynthesisStatus.RUNNING, SynthesisStatus.FAILED);
    }

    @Test
    void userProfileDefaultIsIdle() {
        UserProfile profile = new UserProfile();
        assertThat(profile.getSynthesisStatus()).isEqualTo(SynthesisStatus.IDLE);
    }
}
