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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChannelSelectorTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ChannelModelRepository channelModelRepository;
    @Mock
    private ChannelStatsService channelStatsService;
    @Mock
    private ChannelCircuitBreaker channelCircuitBreaker;
    @Mock
    private ChannelRpmGuard channelRpmGuard;

    private ChannelSelector channelSelector;

    @BeforeEach
    void setUp() {
        channelSelector = new ChannelSelector(
            channelRepository, channelModelRepository, channelStatsService,
            channelCircuitBreaker, channelRpmGuard);
        when(channelStatsService.getHealthFactor(anyLong())).thenReturn(1.0);
        when(channelCircuitBreaker.isOpen(anyLong())).thenReturn(false);
        when(channelRpmGuard.isAvailable(any())).thenReturn(true);
    }

    @Test
    void selectBestWithModel_throwsModelNotFoundWhenUnknown() {
        when(channelModelRepository.existsByModelNameOrAlias("unknown-model")).thenReturn(false);

        GatewayException ex = assertThrows(GatewayException.class,
            () -> channelSelector.selectBestWithModel("unknown-model"));

        assertEquals(GatewayError.MODEL_NOT_FOUND, ex.getError());
    }

    @Test
    void selectBestWithModel_throwsNoAvailableChannelWhenDisabled() {
        when(channelModelRepository.existsByModelNameOrAlias("deepseek-chat")).thenReturn(true);
        when(channelModelRepository.findByModelNameOrAliasAndEnabled("deepseek-chat"))
            .thenReturn(List.of());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> channelSelector.selectBestWithModel("deepseek-chat"));

        assertEquals(GatewayError.NO_AVAILABLE_CHANNEL, ex.getError());
    }

    @Test
    void selectBestWithModel_returnsActiveChannel() {
        Channel channel = new Channel();
        channel.setId(1L);
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setDeleted((short) 0);
        channel.setPriority(1);
        channel.setWeight(1);

        ChannelModel model = new ChannelModel();
        model.setChannelId(1L);
        model.setModelName("deepseek-chat");

        when(channelModelRepository.existsByModelNameOrAlias("deepseek-chat")).thenReturn(true);
        when(channelModelRepository.findByModelNameOrAliasAndEnabled("deepseek-chat"))
            .thenReturn(List.of(model));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channel));

        ChannelSelection selection = channelSelector.selectBestWithModel("deepseek-chat");

        assertEquals(channel, selection.channel());
        assertEquals(model, selection.model());
    }

    @Test
    void selectAllForModel_returnsSortedSelections() {
        Channel channelA = activeChannel(1L, 5, 1);
        Channel channelB = activeChannel(2L, 10, 1);
        ChannelModel modelA = modelFor(1L);
        ChannelModel modelB = modelFor(2L);

        when(channelModelRepository.existsByModelNameOrAlias("deepseek-chat")).thenReturn(true);
        when(channelModelRepository.findByModelNameOrAliasAndEnabled("deepseek-chat"))
            .thenReturn(List.of(modelA, modelB));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channelA));
        when(channelRepository.findById(2L)).thenReturn(Optional.of(channelB));

        List<ChannelSelection> selections = channelSelector.selectAllForModel("deepseek-chat");

        assertEquals(2, selections.size());
        assertEquals(2L, selections.getFirst().channel().getId());
    }

    @Test
    void selectForModel_circuitOpen_skipsChannel() {
        Channel openChannel = activeChannel(1L, 10, 1);
        Channel closedChannel = activeChannel(2L, 5, 1);
        ChannelModel modelA = modelFor(1L);
        ChannelModel modelB = modelFor(2L);

        when(channelModelRepository.existsByModelNameOrAlias("deepseek-chat")).thenReturn(true);
        when(channelModelRepository.findByModelNameOrAliasAndEnabled("deepseek-chat"))
            .thenReturn(List.of(modelA, modelB));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(openChannel));
        when(channelRepository.findById(2L)).thenReturn(Optional.of(closedChannel));
        when(channelCircuitBreaker.isOpen(1L)).thenReturn(true);
        when(channelCircuitBreaker.isOpen(2L)).thenReturn(false);

        List<ChannelSelection> selections = channelSelector.selectAllForModel("deepseek-chat");

        assertEquals(1, selections.size());
        assertEquals(2L, selections.getFirst().channel().getId());
    }

    @Test
    void selectForModel_allCircuitOpen_throwsNoChannel() {
        Channel channelA = activeChannel(1L, 10, 1);
        Channel channelB = activeChannel(2L, 5, 1);
        ChannelModel modelA = modelFor(1L);
        ChannelModel modelB = modelFor(2L);

        when(channelModelRepository.existsByModelNameOrAlias("deepseek-chat")).thenReturn(true);
        when(channelModelRepository.findByModelNameOrAliasAndEnabled("deepseek-chat"))
            .thenReturn(List.of(modelA, modelB));
        when(channelRepository.findById(1L)).thenReturn(Optional.of(channelA));
        when(channelRepository.findById(2L)).thenReturn(Optional.of(channelB));
        when(channelCircuitBreaker.isOpen(anyLong())).thenReturn(true);

        GatewayException ex = assertThrows(GatewayException.class,
            () -> channelSelector.selectAllForModel("deepseek-chat"));

        assertEquals(GatewayError.NO_AVAILABLE_CHANNEL, ex.getError());
    }

    private static Channel activeChannel(long id, int priority, int weight) {
        Channel channel = new Channel();
        channel.setId(id);
        channel.setStatus(ChannelStatus.ACTIVE);
        channel.setDeleted((short) 0);
        channel.setPriority(priority);
        channel.setWeight(weight);
        return channel;
    }

    private static ChannelModel modelFor(long channelId) {
        ChannelModel model = new ChannelModel();
        model.setChannelId(channelId);
        model.setModelName("deepseek-chat");
        return model;
    }
}
