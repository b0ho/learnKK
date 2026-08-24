package com.learnkk.session.entity;

import com.learnkk.kernel.domain.CompletionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 멘티 수료 판정 결과(C4, U5 소유). {@code computeCompletion} 이 후보(COMPLETION_CANDIDATE) 판정을,
 * {@code approveMenteeCompletion}(④)이 확정(COMPLETED)을 기록한다. 판정 근거(a/S)를 스냅샷으로 보관한다.
 * status 는 U1 공유 enum {@link CompletionStatus}.
 */
@Entity
@Table(name = "mentee_completion")
@IdClass(MenteeCompletionId.class)
public class MenteeCompletion {

  @Id
  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Id
  @Column(name = "mentee_id", nullable = false)
  private Long menteeId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CompletionStatus status = CompletionStatus.NOT_COMPLETED;

  @Column(name = "attended_count", nullable = false)
  private int attendedCount;

  @Column(name = "total_scheduled", nullable = false)
  private int totalScheduled;

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected MenteeCompletion() {}

  public MenteeCompletion(Long meetingId, Long menteeId) {
    this.meetingId = meetingId;
    this.menteeId = menteeId;
    this.status = CompletionStatus.NOT_COMPLETED;
  }

  /**
   * 80% 판정 결과를 반영한다(BR-U5-4, 정수식 {@code a*100 >= 80*S}). S&gt;0 이고 기준 충족이면 수료후보, 아니면 미수료.
   * 이미 확정(COMPLETED)된 건은 재판정하지 않는다(종료 상태).
   */
  public void applyJudgement(int attended, int scheduled) {
    // 확정(COMPLETED)된 멘티는 종료 상태 — 근거 스냅샷(a/S)을 재계산으로 덮어쓰지 않는다(BR-U5-3/W4).
    if (this.status == CompletionStatus.COMPLETED) {
      return;
    }
    this.attendedCount = attended;
    this.totalScheduled = scheduled;
    boolean candidate = scheduled > 0 && attended * 100 >= 80 * scheduled;
    this.status = candidate ? CompletionStatus.COMPLETION_CANDIDATE : CompletionStatus.NOT_COMPLETED;
  }

  /** ④ 관리자 수료 확정(BR-U5-5). 스냅샷(a/S)은 유지하고 확정 시각을 기록한다. */
  public void approve(OffsetDateTime now) {
    this.status = CompletionStatus.COMPLETED;
    this.approvedAt = now;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public Long getMenteeId() {
    return menteeId;
  }

  public CompletionStatus getStatus() {
    return status;
  }

  public int getAttendedCount() {
    return attendedCount;
  }

  public int getTotalScheduled() {
    return totalScheduled;
  }

  public OffsetDateTime getApprovedAt() {
    return approvedAt;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
