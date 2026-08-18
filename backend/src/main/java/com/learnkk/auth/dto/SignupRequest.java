package com.learnkk.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Signup payload. {@code role} must be MENTOR or MENTEE at the API boundary; ADMIN is rejected by
 * the service (ADMIN_SIGNUP_FORBIDDEN).
 */
public record SignupRequest(
    @NotBlank(message = "닉네임은 필수입니다.") @Size(max = 50, message = "닉네임은 50자 이하여야 합니다.")
        String nickname,
    @NotBlank(message = "비밀번호는 필수입니다.") @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다.")
        String password,
    @NotBlank(message = "사번은 필수입니다.") @Size(max = 50, message = "사번은 50자 이하여야 합니다.")
        String employeeNo,
    @NotBlank(message = "역할은 필수입니다.") String role) {}
