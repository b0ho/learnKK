package com.learnkk.content.dto;

import jakarta.validation.constraints.NotBlank;

/** Notice creation payload (US-4.3). */
public record NoticeCreateRequest(@NotBlank(message = "공지 내용은 필수입니다.") String body) {}
