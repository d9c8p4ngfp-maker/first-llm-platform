package com.first.gateway.service.channel;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.enums.ChannelStatus;
import com.first.gateway.domain.enums.ModelTier;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.security.UpstreamUrlValidator;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.web.admin.dto.ChannelRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ChannelServiceImpl implements ChannelService {

    private final ChannelRepository channelRepository;
    private final ChannelModelRepository channelModelRepository;
    private final ChannelKeyCrypto channelKeyCrypto;

    public ChannelServiceImpl(ChannelRepository channelRepository,
                              ChannelModelRepository channelModelRepository,
                              ChannelKeyCrypto channelKeyCrypto) {
        this.channelRepository = channelRepository;
        this.channelModelRepository = channelModelRepository;
        this.channelKeyCrypto = channelKeyCrypto;
    }

    @Override
    public List<Channel> listEnabled() {
        return channelRepository.findByStatusOrderByPriorityDescWeightDesc(ChannelStatus.ACTIVE);
    }

    @Override
    public List<Channel> listAll() {
        return channelRepository.findByDeletedOrderByPriorityDescWeightDesc((short) 0);
    }

    @Override
    public List<Channel> listByUserId(Long userId) {
        return channelRepository.findByUserIdAndDeletedOrderByPriorityDescWeightDesc(userId, (short) 0);
    }

    @Override
    public Optional<Channel> findById(Long id) {
        return channelRepository.findById(id);
    }

    @Override
    public Channel requireInTenant(Long id, Long tenantId) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "channel not found"));
        if (channel.getDeleted() != 0 || !tenantId.equals(channel.getTenantId())) {
            throw new GatewayException(GatewayError.ACCESS_DENIED);
        }
        return channel;
    }

    @Override
    @Transactional
    public Channel save(Channel channel) {
        return channelRepository.save(channel);
    }

    @Override
    @Transactional
    public Channel createFromRequest(ChannelRequest request, Long tenantId) {
        if (request.name() == null || request.name().isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "name required");
        }
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "apiKey required");
        }
        if (request.baseUrl() == null || request.baseUrl().isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "baseUrl required");
        }
        UpstreamUrlValidator.validate(request.baseUrl());
        Channel channel = new Channel();
        channel.setTenantId(tenantId);
        channel.setName(request.name());
        channel.setType(request.type() != null ? request.type() : "OPENAI");
        channel.setProvider(request.provider());
        channel.setBaseUrl(request.baseUrl());
        channel.setApiKeyEncrypted(channelKeyCrypto.encrypt(request.apiKey()));
        channel.setPriority(request.priority() != null ? request.priority() : 0);
        channel.setWeight(request.weight() != null ? request.weight() : 1);
        channel.setStatus(request.status() != null ? request.status() : ChannelStatus.ACTIVE);
        channel.setMaxRpm(request.maxRpm() != null ? request.maxRpm() : 0);
        channel.setConfig(request.config());
        channel = channelRepository.save(channel);
        seedDefaultModelsIfEmpty(channel.getId());
        return channel;
    }

    @Override
    @Transactional
    public Channel createFromRequestForUser(Long tenantId, Long userId, ChannelRequest request) {
        Channel channel = createFromRequest(request, tenantId);
        channel.setUserId(userId);
        return channelRepository.save(channel);
    }

    @Override
    public Channel requireOwnedByUser(Long id, Long userId) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "channel not found"));
        if (channel.getDeleted() != 0 || !userId.equals(channel.getUserId())) {
            throw new GatewayException(GatewayError.ACCESS_DENIED);
        }
        return channel;
    }

    @Override
    @Transactional
    public Channel update(Long id, Channel channel) {
        Channel existing = channelRepository.findById(id)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "channel not found"));
        if (channel.getName() != null) {
            existing.setName(channel.getName());
        }
        if (channel.getType() != null) {
            existing.setType(channel.getType());
        }
        if (channel.getProvider() != null) {
            existing.setProvider(channel.getProvider());
        }
        if (channel.getBaseUrl() != null) {
            UpstreamUrlValidator.validate(channel.getBaseUrl());
            existing.setBaseUrl(channel.getBaseUrl());
        }
        if (channel.getApiKeyEncrypted() != null) {
            existing.setApiKeyEncrypted(channel.getApiKeyEncrypted());
        }
        if (channel.getPriority() != null) {
            existing.setPriority(channel.getPriority());
        }
        if (channel.getWeight() != null) {
            existing.setWeight(channel.getWeight());
        }
        if (channel.getStatus() != null) {
            existing.setStatus(channel.getStatus());
            if (channel.getStatus() == ChannelStatus.ACTIVE) {
                existing.setFailCount(0);
            }
        }
        if (channel.getMaxRpm() != null) {
            existing.setMaxRpm(channel.getMaxRpm());
        }
        if (channel.getConfig() != null) {
            existing.setConfig(channel.getConfig());
        }
        return channelRepository.save(existing);
    }

    @Override
    @Transactional
    public Channel updateFromRequest(Long id, ChannelRequest request) {
        Channel existing = channelRepository.findById(id)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "channel not found"));
        if (request.name() != null) {
            existing.setName(request.name());
        }
        if (request.type() != null) {
            existing.setType(request.type());
        }
        if (request.provider() != null) {
            existing.setProvider(request.provider());
        }
        if (request.baseUrl() != null) {
            UpstreamUrlValidator.validate(request.baseUrl());
            existing.setBaseUrl(request.baseUrl());
        }
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            existing.setApiKeyEncrypted(channelKeyCrypto.encrypt(request.apiKey()));
        }
        if (request.priority() != null) {
            existing.setPriority(request.priority());
        }
        if (request.weight() != null) {
            existing.setWeight(request.weight());
        }
        if (request.status() != null) {
            existing.setStatus(request.status());
            if (request.status() == ChannelStatus.ACTIVE) {
                existing.setFailCount(0);
            }
        }
        if (request.maxRpm() != null) {
            existing.setMaxRpm(request.maxRpm());
        }
        if (request.config() != null) {
            existing.setConfig(request.config());
        }
        return channelRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Channel existing = channelRepository.findById(id)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "channel not found"));
        existing.setDeleted((short) 1);
        existing.setStatus(ChannelStatus.DISABLED);
        channelRepository.save(existing);
    }

    @Override
    public List<ChannelModel> listEnabledModels() {
        return channelModelRepository.findByEnabledOrderByModelNameAsc((short) 1);
    }

    private void seedDefaultModelsIfEmpty(Long channelId) {
        if (!channelModelRepository.findByChannelIdAndEnabled(channelId, (short) 1).isEmpty()) {
            return;
        }
        saveDefaultModel(channelId, "deepseek-chat", "deepseek-chat", ModelTier.STANDARD);
        saveDefaultModel(channelId, "deepseek-reasoner", "deepseek-reasoner", ModelTier.PREMIUM);
    }

    private void saveDefaultModel(Long channelId, String modelName, String modelAlias, ModelTier tier) {
        ChannelModel model = new ChannelModel();
        model.setChannelId(channelId);
        model.setModelName(modelName);
        model.setModelAlias(modelAlias);
        model.setInputRatio(BigDecimal.ONE);
        model.setOutputRatio(BigDecimal.ONE);
        model.setTier(tier);
        model.setEnabled((short) 1);
        channelModelRepository.save(model);
    }
}
