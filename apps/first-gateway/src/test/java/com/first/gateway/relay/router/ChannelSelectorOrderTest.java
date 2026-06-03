package com.first.gateway.relay.router;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.enums.ChannelStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelSelectorOrderTest {

    @Test
    void orderForRetry_putsHigherPriorityFirst() {
        ChannelSelection low = selection(1L, 1, 10);
        ChannelSelection high = selection(2L, 10, 1);

        List<ChannelSelection> ordered = ChannelSelector.orderForRetry(List.of(low, high));

        assertEquals(2L, ordered.getFirst().channel().getId());
        assertEquals(2, ordered.size());
    }

    @Test
    void orderForRetry_includesAllCandidatesInSamePriorityGroup() {
        ChannelSelection a = selection(1L, 5, 1);
        ChannelSelection b = selection(2L, 5, 100);

        List<ChannelSelection> ordered = ChannelSelector.orderForRetry(List.of(a, b));

        assertEquals(2, ordered.size());
        assertTrue(ordered.stream().anyMatch(s -> s.channel().getId() == 1L));
        assertTrue(ordered.stream().anyMatch(s -> s.channel().getId() == 2L));
    }

    private static ChannelSelection selection(long id, int priority, int weight) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setDeleted((short) 0);
        channel.setPriority(priority);
        channel.setWeight(weight);
        ChannelModel model = new ChannelModel();
        model.setChannelId(id);
        model.setModelName("deepseek-chat");
        return new ChannelSelection(channel, model);
    }
}
