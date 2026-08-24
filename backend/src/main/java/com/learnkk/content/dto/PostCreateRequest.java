package com.learnkk.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Week-post creation payload (US-4.1a). Attachments are uploaded separately (multipart). */
public record PostCreateRequest(
    @NotNull(message = "주차는 필수입니다.") @Positive(message = "주차는 1 이상이어야 합니다.") Integer week,
    @NotBlank(message = "본문은 필수입니다.") String body) {}
