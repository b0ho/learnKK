package com.learnkk.meeting.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Recruitment-confirmation payload (T3/T4). {@code proceed=true} advances RECRUITING -&gt;
 * READY_TO_START; {@code proceed=false} cancels to CANCELLED and records {@code reason} (reusing
 * the reject_reason column). The reason is required when cancelling; validation is enforced in the
 * service so the same field serves both branches.
 */
public record ConfirmRecruitmentRequest(
    @NotNull(message = "진행 여부는 필수입니다.") Boolean proceed, String reason) {}
