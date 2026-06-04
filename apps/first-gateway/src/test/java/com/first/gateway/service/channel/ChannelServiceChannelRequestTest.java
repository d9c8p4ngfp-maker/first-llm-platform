package com.first.gateway.service.channel;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.enums.ChannelStatus;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.web.admin.dto.ChannelRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelServiceChannelRequestTest {

    @Mock
    private ChannelRepository channelRepository;
    @Mock
    private ChannelModelRepository channelModelRepository;
    @Mock
    private ChannelKeyCrypto channelKeyCrypto;

    private ChannelServiceImpl channelService;

    @BeforeEach
    void setUp() {
        channelService = new ChannelServiceImpl(channelRepository, channelModelRepository, channelKeyCrypto);
    }

    @Test
    void createFromRequest_encryptsPlainApiKey() {
        when(channelKeyCrypto.encrypt("sk-test")).thenReturn("enc-sk-test");
        when(channelRepository.save(any(Channel.class))).thenAnswer(inv -> {
            Channel c = inv.getArgument(0);
            c.setId(99L);
            return c;
        });

        ChannelRequest req = new ChannelRequest(
            "Test", "OPENAI", "deepseek", "https://api.example.com", "sk-test",
            1, 2, ChannelStatus.ACTIVE, 100, null);

        Channel saved = channelService.createFromRequest(req, 1L);

        assertEquals("enc-sk-test", saved.getApiKeyEncrypted());
        assertEquals("Test", saved.getName());
        verify(channelKeyCrypto).encrypt("sk-test");
    }

    @Test
    void createFromRequest_blankApiKeyRejected() {
        ChannelRequest req = new ChannelRequest(
            "Test", "OPENAI", null, "https://api.example.com", "  ",
            null, null, null, null, null);

        GatewayException ex = assertThrows(GatewayException.class, () -> channelService.createFromRequest(req, 1L));
        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void updateFromRequest_reencryptsWhenApiKeyProvided() {
        Channel existing = new Channel();
        existing.setId(1L);
        existing.setApiKeyEncrypted("old-enc");
        when(channelRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(channelKeyCrypto.encrypt("sk-new")).thenReturn("new-enc");
        when(channelRepository.save(existing)).thenReturn(existing);

        channelService.updateFromRequest(1L, new ChannelRequest(
            null, null, null, null, "sk-new", null, null, null, null, null));

        assertEquals("new-enc", existing.getApiKeyEncrypted());
    }

    @Test
    void updateFromRequest_blankApiKeyKeepsOld() {
        Channel existing = new Channel();
        existing.setId(1L);
        existing.setApiKeyEncrypted("old-enc");
        when(channelRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(channelRepository.save(existing)).thenReturn(existing);

        channelService.updateFromRequest(1L, new ChannelRequest(
            "Renamed", null, null, null, "", null, null, null, null, null));

        assertEquals("old-enc", existing.getApiKeyEncrypted());
        assertEquals("Renamed", existing.getName());
    }

    @Test
    void listAll_includesDisabled() {
        Channel active = new Channel();
        active.setStatus(ChannelStatus.ACTIVE);
        Channel disabled = new Channel();
        disabled.setStatus(ChannelStatus.DISABLED);
        when(channelRepository.findByDeletedOrderByPriorityDescWeightDesc((short) 0))
            .thenReturn(List.of(active, disabled));

        List<Channel> all = channelService.listAll();

        assertEquals(2, all.size());
    }

    @Test
    void listEnabled_excludesDisabled() {
        Channel active = new Channel();
        active.setStatus(ChannelStatus.ACTIVE);
        when(channelRepository.findByStatusOrderByPriorityDescWeightDesc(ChannelStatus.ACTIVE))
            .thenReturn(List.of(active));

        assertEquals(1, channelService.listEnabled().size());
    }
}