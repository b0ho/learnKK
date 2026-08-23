package com.learnkk.enrollment.domain;

/**
 * Enrollment lifecycle status (U4-local). Distinct from {@link
 * com.learnkk.kernel.domain.MeetingStatus} — this is the state of the application itself, not the
 * meeting. Physical representation is varchar + CHECK in the DB.
 */
public enum EnrollmentStatus {
  APPLIED,
  CANCELLED
}
