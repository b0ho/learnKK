package com.learnkk.content.entity;

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
 * A week's post authored by the owning mentor (C5, U6). Body is required; attachments are optional
 * (0..n) and held by {@link PostAttachment}. {@code meetingId}/{@code authorId} are foreign keys by
 * id only — mentor ownership is read from U3 and never crosses into the meetings table here.
 */
@Entity
@Table(name = "post")
public class Post {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "meeting_id", nullable = false)
  private Long meetingId;

  @Column(name = "author_id", nullable = false)
  private Long authorId;

  @Column(nullable = false)
  private int week;

  @Column(nullable = false)
  private String body;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  private OffsetDateTime updatedAt;

  protected Post() {}

  public Post(Long meetingId, Long authorId, int week, String body) {
    this.meetingId = meetingId;
    this.authorId = authorId;
    this.week = week;
    this.body = body;
  }

  public Long getId() {
    return id;
  }

  public Long getMeetingId() {
    return meetingId;
  }

  public Long getAuthorId() {
    return authorId;
  }

  public int getWeek() {
    return week;
  }

  public String getBody() {
    return body;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
