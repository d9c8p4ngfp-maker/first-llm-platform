package com.first.gateway.repository;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.enums.ChannelStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

    List<Channel> findByStatusOrderByPriorityDescWeightDesc(ChannelStatus status);

    List<Channel> findByDeletedOrderByPriorityDescWeightDesc(Short deleted);

    List<Channel> findByTypeAndStatus(String type, ChannelStatus status);

    List<Channel> findByUserIdAndDeletedOrderByPriorityDescWeightDesc(Long userId, Short deleted);

    List<Channel> findByTenantIdAndDeleted(Long tenantId, Short deleted);
}
