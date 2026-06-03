package com.first.gateway.relay.router;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.enums.ChannelStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.service.channel.ChannelRpmGuard;
import com.first.gateway.service.channel.ChannelStatsService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

@Component
public class ChannelSelector {

    private final ChannelRepository channelRepository;
    private final ChannelModelRepository channelModelRepository;
    private final ChannelStatsService channelStatsService;
    private final ChannelCircuitBreaker channelCircuitBreaker;
    private final ChannelRpmGuard channelRpmGuard;

    public ChannelSelector(ChannelRepository channelRepository,
                           ChannelModelRepository channelModelRepository,
                           ChannelStatsService channelStatsService,
                           ChannelCircuitBreaker channelCircuitBreaker,
                           ChannelRpmGuard channelRpmGuard) {
        this.channelRepository = channelRepository;
        this.channelModelRepository = channelModelRepository;
        this.channelStatsService = channelStatsService;
        this.channelCircuitBreaker = channelCircuitBreaker;
        this.channelRpmGuard = channelRpmGuard;
    }

    public Channel selectForModel(String model) {
        return selectBestWithModel(model).channel();
    }

    public Channel selectForModel(String model, Long userId) {
        return selectBestWithModel(model, userId).channel();
    }

    public ChannelSelection selectBestWithModel(String model) {
        return selectAllForModel(model).getFirst();
    }

    public ChannelSelection selectBestWithModel(String model, Long userId) {
        return selectAllForModel(model, userId).getFirst();
    }

    public List<ChannelSelection> selectAllForModel(String model) {
        return selectAllForModel(model, null);
    }

    public List<ChannelSelection> selectAllForModel(String model, Long userId) {
        if (!channelModelRepository.existsByModelNameOrAlias(model)) {
            throw new GatewayException(GatewayError.MODEL_NOT_FOUND);
        }
        List<ChannelModel> models = channelModelRepository.findByModelNameOrAliasAndEnabled(model);
        if (models.isEmpty()) {
            throw new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL);
        }
        List<ChannelSelection> selections = models.stream()
            .map(cm -> channelRepository.findById(cm.getChannelId())
                .filter(ch -> isSelectableForUser(ch, userId))
                .map(ch -> new ChannelSelection(ch, cm)))
            .flatMap(Optional::stream)
            .toList();
        if (selections.isEmpty()) {
            throw new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL);
        }
        return orderForRetry(selections, channelStatsService::getHealthFactor);
    }

    public Channel selectBestForModel(String model) {
        return selectBestWithModel(model).channel();
    }

    private boolean isSelectableForUser(Channel channel, Long userId) {
        if (!isSelectable(channel)) {
            return false;
        }
        if (userId == null) {
            return true;
        }
        return userId.equals(channel.getUserId());
    }

    private boolean isSelectable(Channel channel) {
        if (channel == null
            || channel.getDeleted() != 0
            || (channel.getStatus() != ChannelStatus.ACTIVE && channel.getStatus() != ChannelStatus.TESTING)) {
            return false;
        }
        if (channelCircuitBreaker.isOpen(channel.getId())) {
            return false;
        }
        return channelRpmGuard.isAvailable(channel);
    }

    static List<ChannelSelection> orderForRetry(List<ChannelSelection> selections,
                                                ToDoubleFunction<Long> healthFactorFn) {
        Map<Integer, List<ChannelSelection>> byPriority = selections.stream()
            .collect(Collectors.groupingBy(
                s -> s.channel().getPriority(),
                LinkedHashMap::new,
                Collectors.toList()));

        List<Integer> priorities = byPriority.keySet().stream()
            .sorted(Comparator.reverseOrder())
            .toList();

        List<ChannelSelection> ordered = new ArrayList<>();
        for (Integer priority : priorities) {
            List<ChannelSelection> group = new ArrayList<>(byPriority.get(priority));
            ChannelSelection first = weightedPick(group, healthFactorFn);
            ordered.add(first);
            group.remove(first);
            group.sort(Comparator.<ChannelSelection>comparingInt(s -> effectiveWeight(s, healthFactorFn)).reversed());
            ordered.addAll(group);
        }
        return ordered;
    }

    static List<ChannelSelection> orderForRetry(List<ChannelSelection> selections) {
        return orderForRetry(selections, id -> 1.0);
    }

    static ChannelSelection weightedPick(List<ChannelSelection> group) {
        return weightedPick(group, id -> 1.0);
    }

    static ChannelSelection weightedPick(List<ChannelSelection> group, ToDoubleFunction<Long> healthFactorFn) {
        if (group.size() == 1) {
            return group.getFirst();
        }
        int totalWeight = 0;
        int[] effectiveWeights = new int[group.size()];
        for (int i = 0; i < group.size(); i++) {
            effectiveWeights[i] = effectiveWeight(group.get(i), healthFactorFn);
            totalWeight += effectiveWeights[i];
        }
        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (int i = 0; i < group.size(); i++) {
            cumulative += effectiveWeights[i];
            if (roll < cumulative) {
                return group.get(i);
            }
        }
        return group.getLast();
    }

    private static int effectiveWeight(ChannelSelection selection, ToDoubleFunction<Long> healthFactorFn) {
        double health = healthFactorFn.applyAsDouble(selection.channel().getId());
        return Math.max(1, (int) (Math.max(selection.channel().getWeight(), 1) * health));
    }
}
