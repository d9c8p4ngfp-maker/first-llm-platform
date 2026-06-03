package com.first.gateway.relay.router;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;

public record ChannelSelection(Channel channel, ChannelModel model) {}
