package com.first.gateway.web.admin.dto;

import com.first.gateway.domain.enums.UserStatus;

public record UpdateUserStatusRequest(UserStatus status) {}
