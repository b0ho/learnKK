package com.learnkk.meeting.entity;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.MentorCompletionStatus;
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

/** A mentoring meeting. Lifecycle is owned by the meeting state machine (ADR-006). */
@Entity
@Table(name = "meetings")
public class Meeting {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "mentor_id", nullable = false)
  private Long mentorId;

  @Column(nullable = false)
  private String title;

  @Column private String topic;

  @Column(nullable = false)
  private int weeks;

  @Column(name = "recruit_start")
  private OffsetDateTime recruitStart;

  @Column(name = "recruit_end")
  private OffsetDateTime recruitEnd;

  @Column(nullable = false)
  private int capacity;

  @Column private String format;

  @Column(name = "initial_content", columnDefinition = "text")
  private String initialContent;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MeetingStatus status = MeetingStatus.PENDING_APPROVAL;

  @Column(name = "reject_reason")
  private String rejectReason;

  /** 멘토 수료 판정(FR-7): 관리자 판단만으로 결정. 초기값 PENDING. */
  @Enumerated(EnumType.STRING)
  @Column(name = "mentor_completion_status", nullable = false)
  private MentorCompletionStatus mentorCompletionStatus = MentorCompletionStatus.PENDING;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected Meeting() {}

  public Meeting(
      Long mentorId,
      String title,
      String topic,
      int weeks,
      OffsetDateTime recruitStart,
      OffsetDateTime recruitEnd,
      int capacity,
      String format,
      String initialContent) {
    this.mentorId = mentorId;
    this.title = title;
    this.topic = topic;
    this.weeks = weeks;
    this.recruitStart = recruitStart;
    this.recruitEnd = recruitEnd;
    this.capacity = capacity;
    this.format = format;
    this.initialContent = initialContent;
    this.status = MeetingStatus.PENDING_APPROVAL;
  }

  public Long getId() {
    return id;
  }

  public Long getMentorId() {
    return mentorId;
  }

  public String getTitle() {
    return title;
  }

  public String getTopic() {
    return topic;
  }

  public int getWeeks() {
    return weeks;
  }

  public OffsetDateTime getRecruitStart() {
    return recruitStart;
  }

  public OffsetDateTime getRecruitEnd() {
    return recruitEnd;
  }

  public int getCapacity() {
    return capacity;
  }

  public String getFormat() {
    return format;
  }

  public String getInitialContent() {
    return initialContent;
  }

  public MeetingStatus getStatus() {
    return status;
  }

  public void setStatus(MeetingStatus status) {
    this.status = status;
  }

  public String getRejectReason() {
    return rejectReason;
  }

  public void setRejectReason(String rejectReason) {
    this.rejectReason = rejectReason;
  }

  public MentorCompletionStatus getMentorCompletionStatus() {
    return mentorCompletionStatus;
  }

  public void setMentorCompletionStatus(MentorCompletionStatus mentorCompletionStatus) {
    this.mentorCompletionStatus = mentorCompletionStatus;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
