package com.learnkk.messaging.dto;

import com.learnkk.kernel.domain.Role;

/** A user the caller is permitted to message — supports the FE recipient picker. */
public record RecipientResponse(Long userId, String nickname, Role role) {}
