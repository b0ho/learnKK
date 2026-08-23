package com.learnkk.survey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A mentee's course feedback for a meeting (C7, U8). Meeting and user are referenced by id only —
 * no ORM associations cross module boundaries. One row per (meeting, mentee) — re-submission
 * updates {@code content} in place (BR-U8-3).
 */
@Entity
@Table(name = "feedback")
public class Feedback {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Column(name = "mentee_id", nullable = false)
  private Long menteeId;

  @Column(nullable = false)
  private String content;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Feedback() {}

  public Feedback(Long meetingId, Long menteeId, String content) {
    this.meetingId = meetingId;
    this.menteeId = menteeId;
    this.content = content;
  }

  /** Re-submission: overwrite the stored feedback content (idempotent upsert, BR-U8-3). */
  public void updateContent(String content) {
    this.content = content;
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

  public String getContent() {
    return content;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
