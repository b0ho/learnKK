package com.learnkk.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A file attached to a {@link Post}, stored as a PostgreSQL {@code bytea} BLOB (ADR-004, TD-U6-1).
 * The binary {@code data} is a {@code bytea} column ({@link SqlTypes#VARBINARY}); metadata listings
 * avoid loading it by selecting a projection (see {@code PostAttachmentRepository}). It is read in
 * full only on download. Content type and size are validated against the whitelist / 20MB cap
 * (BR-U6-2) before the row is created.
 */
@Entity
@Table(name = "post_attachment")
public class PostAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "post_id", nullable = false)
  private Long postId;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size_bytes", nullable = false)
  private long sizeBytes;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(nullable = false)
  private byte[] data;

  @Column(name = "uploader_id")
  private Long uploaderId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private OffsetDateTime createdAt;

  protected PostAttachment() {}

  public PostAttachment(
      Long postId,
      String fileName,
      String contentType,
      long sizeBytes,
      byte[] data,
      Long uploaderId) {
    this.postId = postId;
    this.fileName = fileName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    this.data = data;
    this.uploaderId = uploaderId;
  }

  public Long getId() {
    return id;
  }

  public Long getPostId() {
    return postId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSizeBytes() {
    return sizeBytes;
  }

  public byte[] getData() {
    return data;
  }

  public Long getUploaderId() {
    return uploaderId;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
