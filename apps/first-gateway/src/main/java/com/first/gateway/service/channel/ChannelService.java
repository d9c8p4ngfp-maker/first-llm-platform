package com.first.gateway.service.channel;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.web.admin.dto.ChannelRequest;

import java.util.List;
import java.util.Optional;

public interface ChannelService {

    List<Channel> listEnabled();

    List<Channel> listAll();

    List<Channel> listByUserId(Long userId);

    List<ChannelModel> listEnabledModels();

    Optional<Channel> findById(Long id);

    Channel requireInTenant(Long id, Long tenantId);

    Channel save(Channel channel);

    Channel createFromRequest(ChannelRequest request, Long tenantId);

    Channel createFromRequestForUser(Long tenantId, Long userId, ChannelRequest request);

    Channel requireOwnedByUser(Long id, Long userId);

    Channel update(Long id, Channel channel);

    Channel updateFromRequest(Long id, ChannelRequest request);

    void delete(Long id);

    Channel cloneForUser(Channel source, Long tenantId, Long userId);

    void disableAllForUser(Long userId);
}
