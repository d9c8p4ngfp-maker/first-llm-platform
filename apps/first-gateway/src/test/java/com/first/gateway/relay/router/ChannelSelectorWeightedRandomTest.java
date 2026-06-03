package com.first.gateway.relay.router;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.enums.ChannelStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelSelectorWeightedRandomTest {

    private static final int SAMPLES = 10_000;
    private static final double TOLERANCE = 0.05;

    @Test
    void weightedPick_followsDistribution() {
        ChannelSelection heavy = selection(1L, 70);
        ChannelSelection light = selection(2L, 30);

        Map<Long, Long> counts = countPicks(List.of(heavy, light));

        double heavyRatio = counts.get(1L) / (double) SAMPLES;
        assertTrue(heavyRatio >= 0.65 && heavyRatio <= 0.75,
            "expected ~70% for weight=70, got " + heavyRatio);
    }

    @Test
    void weightedPick_equalWeights() {
        ChannelSelection a = selection(1L, 1);
        ChannelSelection b = selection(2L, 1);

        Map<Long, Long> counts = countPicks(List.of(a, b));

        double ratioA = counts.get(1L) / (double) SAMPLES;
        assertTrue(ratioA >= 0.45 && ratioA <= 0.55,
            "expected ~50% for equal weights, got " + ratioA);
    }

    @Test
    void weightedPick_singleChannel() {
        ChannelSelection only = selection(1L, 10);

        Map<Long, Long> counts = countPicks(List.of(only));

        assertEquals(SAMPLES, counts.get(1L));
    }

    @Test
    void weightedPick_zeroWeightClampedToOne() {
        ChannelSelection zero = selection(1L, 0);
        ChannelSelection heavy = selection(2L, 10);

        Map<Long, Long> counts = countPicks(List.of(zero, heavy));

        double lightRatio = counts.get(1L) / (double) SAMPLES;
        assertTrue(lightRatio >= 0.06 && lightRatio <= 0.12,
            "expected ~9% for clamped weight=1 vs 10, got " + lightRatio);
    }

    private static Map<Long, Long> countPicks(List<ChannelSelection> group) {
        return IntStream.range(0, SAMPLES)
            .mapToObj(i -> ChannelSelector.weightedPick(group).channel().getId())
            .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
    }

    private static ChannelSelection selection(long id, int weight) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setDeleted((short) 0);
        channel.setWeight(weight);
        ChannelModel model = new ChannelModel();
        model.setChannelId(id);
        model.setModelName("m");
        return new ChannelSelection(channel, model);
    }

    @Test
    void weightedPick_withHealthFactor_adjustsWeight() {
        ChannelSelection heavy = selection(1L, 10);
        ChannelSelection light = selection(2L, 5);

        Map<Long, Long> counts = countPicksWithHealth(
            List.of(heavy, light),
            id -> id == 1L ? 0.3 : 1.0);

        double ratioB = counts.getOrDefault(2L, 0L) / (double) SAMPLES;
        assertTrue(ratioB > 0.55,
            "expected channel B (health=1.0) selected more often, got " + ratioB);
    }

    @Test
    void weightedPick_allUnhealthy_stillSelects() {
        ChannelSelection a = selection(1L, 5);
        ChannelSelection b = selection(2L, 5);

        Map<Long, Long> counts = countPicksWithHealth(
            List.of(a, b),
            id -> 0.1);

        assertEquals(SAMPLES, counts.getOrDefault(1L, 0L) + counts.getOrDefault(2L, 0L));
    }

    @Test
    void weightedPick_healthFactorOne_noChange() {
        ChannelSelection heavy = selection(1L, 7);
        ChannelSelection light = selection(2L, 3);

        Map<Long, Long> counts = countPicksWithHealth(
            List.of(heavy, light),
            id -> 1.0);

        double heavyRatio = counts.get(1L) / (double) SAMPLES;
        assertTrue(heavyRatio >= 0.65 && heavyRatio <= 0.75,
            "expected ~70% for weight=7 with health=1.0, got " + heavyRatio);
    }

    private static Map<Long, Long> countPicksWithHealth(List<ChannelSelection> group,
                                                        java.util.function.ToDoubleFunction<Long> health) {
        return IntStream.range(0, SAMPLES)
            .mapToObj(i -> ChannelSelector.weightedPick(group, health).channel().getId())
            .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
    }
}