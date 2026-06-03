package com.first.gateway.relay.adapter;

import com.first.gateway.domain.entity.Channel;

import java.util.Map;

public interface LlmAdapter {

    String getType();

    Map<String, Object> chat(Channel channel, Map<String, Object> request);
}
