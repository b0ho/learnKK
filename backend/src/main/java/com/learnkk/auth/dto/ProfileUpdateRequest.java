package com.learnkk.auth.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

/** Profile edit payload. Business limits (tags &le; 10, intro &le; 500) enforced in the service. */
public record ProfileUpdateRequest(
    List<String> tags, @Size(max = 500, message = "소개는 500자 이하여야 합니다.") String intro) {}
