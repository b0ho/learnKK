package com.learnkk.enrollment.entity;

import com.learnkk.enrollment.domain.EnrollmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A mentee's application to a meeting (C3, U4). Meeting capacity and status are owned by U3 and
 * read via {@code MeetingService} — this entity never touches the meetings table directly. Foreign
 * keys are held by id (no ORM associations across module boundaries).
 */
@Entity
@Table(name = "enrollment")
public class Enrollment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Column(name = "mentee_id", nullable = false)
  private Long menteeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EnrollmentStatus status = EnrollmentStatus.APPLIED;

  @Column(name = "applied_at", nullable = false)
  private OffsetDateTime appliedAt;

  @Column(name = "cancelled_at")
  private OffsetDateTime cancelledAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected Enrollment() {}

  public Enrollment(Long meetingId, Long menteeId) {
    this.meetingId = meetingId;
    this.menteeId = menteeId;
    this.status = EnrollmentStatus.APPLIED;
    this.appliedAt = OffsetDateTime.now();
  }

  /** Transition to CANCELLED, recording the cancellation time (BR-U4-3). */
  public void cancel() {
    this.status = EnrollmentStatus.CANCELLED;
    this.cancelledAt = OffsetDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public Long getMenteeId() {
    return menteeId;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public OffsetDateTime getAppliedAt() {
    return appliedAt;
  }

  public OffsetDateTime getCancelledAt() {
    return cancelledAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
