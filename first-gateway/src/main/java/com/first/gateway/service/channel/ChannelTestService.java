package com.first.gateway.service.channel;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.relay.adapter.OpenAiAdapter;
import com.first.gateway.repository.ChannelModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChannelTestService {

    private final OpenAiAdapter openAiAdapter;
    private final ChannelModelRepository channelModelRepository;

    public ChannelTestService(OpenAiAdapter openAiAdapter,
                              ChannelModelRepository channelModelRepository) {
        this.openAiAdapter = openAiAdapter;
        this.channelModelRepository = channelModelRepository;
    }

    public Map<String, Object> test(Channel channel) {
        long started = System.currentTimeMillis();
        List<ChannelModel> models = channelModelRepository.findByChannelIdAndEnabled(channel.getId(), (short) 1);
        if (models.isEmpty()) {
            return Map.of("success", false, "latencyMs", 0, "error", "no enabled model on channel");
        }
        String model = models.getFirst().getModelName();
        try {
            openAiAdapter.chat(channel, Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", "Hi")),
                "max_tokens", 1));
            return Map.of("success", true, "latencyMs", System.currentTimeMillis() - started);
        } catch (GatewayException ex) {
            return Map.of(
                "success", false,
                "latencyMs", System.currentTimeMillis() - started,
                "error", ex.getError().getCode());
        }
    }
}
