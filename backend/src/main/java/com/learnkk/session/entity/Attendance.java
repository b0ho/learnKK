package com.learnkk.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 세션별 멘티 self check-in 출석(C4, U5). {@code unique(session_id, mentee_id)} 로 세션당 1회만 허용해 멱등을
 * 보장한다(BR-U5-2). 세션·유저와의 관계는 id 로만 보유한다.
 */
@Entity
@Table(name = "attendance")
public class Attendance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "session_id", nullable = false)
  private Long sessionId;

  @Column(name = "mentee_id", nullable = false)
  private Long menteeId;

  @Column(name = "checked_in_at", nullable = false)
  private OffsetDateTime checkedInAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected Attendance() {}

  public Attendance(Long sessionId, Long menteeId) {
    this.sessionId = sessionId;
    this.menteeId = menteeId;
    this.checkedInAt = OffsetDateTime.now();
  }

  public Long getId() {
    return id;
  }

  public Long getSessionId() {
    return sessionId;
  }

  public Long getMenteeId() {
    return menteeId;
  }

  public OffsetDateTime getCheckedInAt() {
    return checkedInAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
