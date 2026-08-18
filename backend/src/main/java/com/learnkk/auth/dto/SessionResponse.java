package com.learnkk.auth.dto;

import com.learnkk.kernel.domain.Role;

/** Issued session token plus the caller's role. */
public record SessionResponse(String token, Role role) {}
