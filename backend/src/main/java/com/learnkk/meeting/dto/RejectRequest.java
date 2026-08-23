package com.learnkk.meeting.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Rejection payload. The reason is mandatory (design [assumption]: rejection/cancellation must
 * record a reason) — reused for both meeting rejection (T2) and recruitment cancellation (T4).
 */
public record RejectRequest(@NotBlank(message = "반려 사유는 필수입니다.") String reason) {}
