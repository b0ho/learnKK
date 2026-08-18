package com.learnkk.meeting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A pre-application survey question attached to a meeting. */
@Entity
@Table(name = "survey_questions")
public class SurveyQuestion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Column(name = "order_no", nullable = false)
  private int orderNo;

  @Column(nullable = false)
  private String text;

  @Column(nullable = false)
  private String type;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(columnDefinition = "text[]")
  private List<String> options = new ArrayList<>();

  @Column(nullable = false)
  private boolean required = true;

  protected SurveyQuestion() {}

  public SurveyQuestion(
      Long meetingId,
      int orderNo,
      String text,
      String type,
      List<String> options,
      boolean required) {
    this.meetingId = meetingId;
    this.orderNo = orderNo;
    this.text = text;
    this.type = type;
    this.options = options == null ? new ArrayList<>() : options;
    this.required = required;
  }

  public Long getId() {
    return id;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public int getOrderNo() {
    return orderNo;
  }

  public String getText() {
    return text;
  }

  public String getType() {
    return type;
  }

  public List<String> getOptions() {
    return options;
  }

  public boolean isRequired() {
    return required;
  }
}
