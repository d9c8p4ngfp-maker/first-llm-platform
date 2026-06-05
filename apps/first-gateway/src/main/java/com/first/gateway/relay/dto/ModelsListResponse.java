package com.first.gateway.relay.dto;

import java.util.List;

public record ModelsListResponse(String object, List<ModelObject> data) {}
