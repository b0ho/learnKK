package com.learnkk.enrollment.dto;

import com.learnkk.enrollment.domain.EnrollmentStatus;
import com.learnkk.enrollment.entity.Enrollment;
import java.time.OffsetDateTime;

/** A mentee's own enrollment view (US-3.2 / US-3.5). Meeting details are composed on the FE. */
public record EnrollmentResponse(
    Long id, Long meetingId, Long menteeId, EnrollmentStatus status, OffsetDateTime appliedAt) {

  public static EnrollmentResponse from(Enrollment e) {
    return new EnrollmentResponse(
        e.getId(), e.getMeetingId(), e.getMenteeId(), e.getStatus(), e.getAppliedAt());
  }
}
