package com.learnkk.meeting.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 멘토 수료 판정 요청(FR-7). {@code status} 는 {@code COMPLETED}(수료) 또는 {@code NOT_COMPLETED}(미수료).
 * 관리자만 호출하며, 판단만으로 결정하므로 별도 사유는 요구하지 않는다.
 */
public record MentorCompletionRequest(@NotBlank(message = "판정 상태는 필수입니다.") String status) {}
