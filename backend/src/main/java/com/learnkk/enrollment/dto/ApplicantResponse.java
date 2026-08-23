package com.learnkk.enrollment.dto;

import java.time.OffsetDateTime;

/**
 * An applicant row for the owning mentor's / admin's applicant listing (US-2.3). The mentee
 * nickname is a minimal U2 read (U4-&gt;U2 allowed).
 */
public record ApplicantResponse(Long menteeId, String nickname, OffsetDateTime appliedAt) {}
