package com.first.gateway.domain.enums;

import com.first.gateway.domain.entity.ChannelModel;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ModelTierTest {

    @Test
    void shouldContainStandardAndPremium() {
        assertThat(ModelTier.values())
            .containsExactly(ModelTier.STANDARD, ModelTier.PREMIUM);
    }

    @Test
    void channelModelDefaultTierIsStandard() {
        ChannelModel cm = new ChannelModel();
        assertThat(cm.getTier()).isEqualTo(ModelTier.STANDARD);
    }
}
