package com.first.gateway.service.channel;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.security.UpstreamUrlValidator;
import com.first.gateway.relay.adapter.OpenAiAdapter;
import com.first.gateway.repository.ChannelModelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ChannelTestService {

    private static final Logger log = LoggerFactory.getLogger(ChannelTestService.class);

    private final OpenAiAdapter openAiAdapter;
    private final ChannelModelRepository channelModelRepository;

    public ChannelTestService(OpenAiAdapter openAiAdapter,
                              ChannelModelRepository channelModelRepository) {
        this.openAiAdapter = openAiAdapter;
        this.channelModelRepository = channelModelRepository;
    }

    public Map<String, Object> test(Channel channel) {
        long started = System.currentTimeMillis();
        log.info("Channel test initiated: id={} name={} provider={} baseUrl={}",
            channel.getId(), channel.getName(), channel.getProvider(), channel.getBaseUrl());
        UpstreamUrlValidator.validate(channel.getBaseUrl());
        List<ChannelModel> models = channelModelRepository.findByChannelIdAndEnabled(channel.getId(), (short) 1);
        if (models.isEmpty()) {
            log.warn("Channel test failed: id={} name={} reason=no_enabled_model",
                channel.getId(), channel.getName());
            return Map.of("success", false, "latencyMs", 0, "error", "no enabled model on channel");
        }
        String model = models.getFirst().getModelName();
        log.info("Channel test sending request: channelId={} channelName={} model={}",
            channel.getId(), channel.getName(), model);
        try {
            openAiAdapter.chat(channel, Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", "Hi")),
                "max_tokens", 1));
            long elapsed = System.currentTimeMillis() - started;
            log.info("Channel test success: id={} name={} model={} latencyMs={}",
                channel.getId(), channel.getName(), model, elapsed);
            return Map.of("success", true, "latencyMs", elapsed);
        } catch (GatewayException ex) {
            long elapsed = System.currentTimeMillis() - started;
            log.warn("Channel test failed: id={} name={} model={} error={} latencyMs={}",
                channel.getId(), channel.getName(), model, ex.getError().getCode(), elapsed);
            return Map.of(
                "success", false,
                "latencyMs", elapsed,
                "error", ex.getError().getCode());
        }
    }
}
