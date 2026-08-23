package com.learnkk.content.service;

import com.learnkk.content.domain.AllowedAttachmentType;
import com.learnkk.content.dto.AttachmentResponse;
import com.learnkk.content.entity.Post;
import com.learnkk.content.entity.PostAttachment;
import com.learnkk.content.repository.PostAttachmentRepository;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Attachment upload/download (C5, U6, US-4.1b/4.2). Uploads are gated to the owning mentor of the
 * post's meeting and validated against the format whitelist and 20MB cap (BR-U6-2); downloads are
 * gated to participants (BR-U6-3). Payloads are stored as {@code bytea} (ADR-004).
 */
@Service
public class AttachmentService {

  private final PostAttachmentRepository attachmentRepository;
  private final PostService postService;
  private final ContentAccessService accessService;

  public AttachmentService(
      PostAttachmentRepository attachmentRepository,
      PostService postService,
      ContentAccessService accessService) {
    this.attachmentRepository = attachmentRepository;
    this.postService = postService;
    this.accessService = accessService;
  }

  /**
   * Upload a file to a post (US-4.1b). Owning mentor only (403). Rejects disallowed content types
   * (400 {@code ATTACHMENT_TYPE_NOT_ALLOWED}), files over 20MB (400 {@code ATTACHMENT_TOO_LARGE})
   * and posts already at the attachment cap (409 {@code ATTACHMENT_LIMIT}).
   */
  @Transactional
  public AttachmentResponse upload(Principal principal, Long postId, MultipartFile file) {
    Post post = postService.loadPost(postId);
    accessService.assertOwningMentor(principal, post.getMeetingId());

    if (file == null || file.isEmpty()) {
      throw new ValidationException(ErrorCodes.CONTENT_VALIDATION, "첨부 파일이 비어 있습니다.");
    }
    if (!AllowedAttachmentType.isAllowed(file.getContentType())) {
      throw new ValidationException(ErrorCodes.ATTACHMENT_TYPE_NOT_ALLOWED, "허용되지 않는 파일 형식입니다.");
    }
    if (file.getSize() > AllowedAttachmentType.MAX_SIZE_BYTES) {
      throw new ValidationException(ErrorCodes.ATTACHMENT_TOO_LARGE, "첨부 파일은 20MB를 초과할 수 없습니다.");
    }
    if (attachmentRepository.countByPostId(postId)
        >= AllowedAttachmentType.MAX_ATTACHMENTS_PER_POST) {
      throw new ConflictException(
          ErrorCodes.ATTACHMENT_LIMIT,
          "게시글당 첨부는 최대 " + AllowedAttachmentType.MAX_ATTACHMENTS_PER_POST + "개까지 가능합니다.");
    }

    byte[] data;
    try {
      data = file.getBytes();
    } catch (IOException e) {
      throw new ValidationException(ErrorCodes.CONTENT_VALIDATION, "파일을 읽을 수 없습니다.");
    }

    PostAttachment saved =
        attachmentRepository.save(
            new PostAttachment(
                postId,
                sanitizeFileName(file.getOriginalFilename()),
                normalizeContentType(file.getContentType()),
                file.getSize(),
                data,
                principal.userId()));
    return AttachmentResponse.from(saved);
  }

  /**
   * Load an attachment (with payload) for download, enforcing participant access on every request
   * (BR-U6-3, direct-URL defence). 404 if the attachment or its post no longer exists.
   */
  @Transactional(readOnly = true)
  public PostAttachment download(Principal principal, Long attachmentId) {
    PostAttachment attachment =
        attachmentRepository
            .findById(attachmentId)
            .orElseThrow(
                () -> new NotFoundException(ErrorCodes.ATTACHMENT_NOT_FOUND, "첨부 파일을 찾을 수 없습니다."));
    Post post = postService.loadPost(attachment.getPostId());
    accessService.assertParticipant(principal, post.getMeetingId());
    // Touch the lazy payload inside the transaction so it is available after the session closes.
    attachment.getData();
    return attachment;
  }

  private static String normalizeContentType(String contentType) {
    if (contentType == null) {
      return "application/octet-stream";
    }
    int semicolon = contentType.indexOf(';');
    return (semicolon >= 0 ? contentType.substring(0, semicolon) : contentType).trim();
  }

  /** Strip path segments from the original filename to avoid path-traversal in headers. */
  private static String sanitizeFileName(String original) {
    if (original == null || original.isBlank()) {
      return "attachment";
    }
    String base = original.replace('\\', '/');
    int slash = base.lastIndexOf('/');
    String name = slash >= 0 ? base.substring(slash + 1) : base;
    return name.isBlank() ? "attachment" : name;
  }
}
