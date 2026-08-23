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
 * A mentee's answer to a single pre-application survey question (C7, U8). The question template is
 * owned by U3 (referenced by {@code questionId}); the meeting and user are referenced by id only —
 * no ORM associations cross module boundaries. One row per (question, mentee) — re-submission
 * updates {@code answerText} in place (BR-U8-1).
 */
@Entity
@Table(name = "survey_answer")
public class SurveyAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Column(name = "question_id", nullable = false)
  private Long questionId;

  @Column(name = "mentee_id", nullable = false)
  private Long menteeId;

  @Column(name = "answer_text")
  private String answerText;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected SurveyAnswer() {}

  public SurveyAnswer(Long meetingId, Long questionId, Long menteeId, String answerText) {
    this.meetingId = meetingId;
    this.questionId = questionId;
    this.menteeId = menteeId;
    this.answerText = answerText;
  }

  /** Re-submission: overwrite the stored answer (idempotent upsert, BR-U8-1). */
  public void updateAnswer(String answerText) {
    this.answerText = answerText;
  }

  public Long getId() {
    return id;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public Long getQuestionId() {
    return questionId;
  }

  public Long getMenteeId() {
    return menteeId;
  }

  public String getAnswerText() {
    return answerText;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
