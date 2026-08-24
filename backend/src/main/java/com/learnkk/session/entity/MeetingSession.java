package com.learnkk.session.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 수업 세션(C4, U5). 멘토가 주차별 날짜·시간을 지정하며 주차당 복수 가능(FR6.1). 물리 테이블명은 {@code meeting_session}
 * — auth 토큰 테이블 {@code sessions}(V2)와의 이름 충돌을 피하기 위해 패키지·테이블을 격리한다. 모임(U3)·유저(U2)와의 관계는
 * 모듈 경계를 넘지 않도록 id 로만 보유한다(ORM 연관관계 없음).
 */
@Entity
@Table(name = "meeting_session")
public class MeetingSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Column(nullable = false)
  private int week;

  @Column(name = "scheduled_at", nullable = false)
  private OffsetDateTime scheduledAt;

  @Column(name = "check_in_window_minutes", nullable = false)
  private int checkInWindowMinutes;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected MeetingSession() {}

  public MeetingSession(Long meetingId, int week, OffsetDateTime scheduledAt, int checkInWindowMinutes) {
    this.meetingId = meetingId;
    this.week = week;
    this.scheduledAt = scheduledAt;
    this.checkInWindowMinutes = checkInWindowMinutes;
  }

  /** 출석 유효 시간창의 종료 시각 = {@code scheduledAt + checkInWindowMinutes}(ADR-005). */
  public OffsetDateTime windowEnd() {
    return scheduledAt.plusMinutes(checkInWindowMinutes);
  }

  /**
   * 요청 시점 {@code now} 가 출석 유효 시간창 {@code [scheduledAt, windowEnd]} 안에 있는지 판정한다(BR-U5-2). 스케줄러
   * 없이 checkIn 요청 시점에 비교한다.
   */
  public boolean isWithinCheckInWindow(OffsetDateTime now) {
    return !now.isBefore(scheduledAt) && !now.isAfter(windowEnd());
  }

  /** 예정 일시 변경(A6). 멘티 현황에 반영된다. */
  public void reschedule(OffsetDateTime newScheduledAt) {
    this.scheduledAt = newScheduledAt;
  }

  public Long getId() {
    return id;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public int getWeek() {
    return week;
  }

  public OffsetDateTime getScheduledAt() {
    return scheduledAt;
  }

  public int getCheckInWindowMinutes() {
    return checkInWindowMinutes;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
