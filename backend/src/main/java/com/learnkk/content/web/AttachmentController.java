package com.learnkk.content.web;

import com.learnkk.content.dto.AttachmentResponse;
import com.learnkk.content.entity.PostAttachment;
import com.learnkk.content.service.AttachmentService;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Attachment endpoints: upload to a post (owning mentor, multipart) and download by id
 * (participants). Downloads are served with {@code Content-Disposition: attachment} so browsers
 * never render the payload inline — an XSS defence for HTML/SVG-like content (TD-U6-3).
 */
@RestController
public class AttachmentController {

  private final AttachmentService attachmentService;

  public AttachmentController(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @PostMapping("/api/posts/{postId}/attachments")
  public ResponseEntity<AttachmentResponse> upload(
      @AuthPrincipal Principal principal,
      @PathVariable Long postId,
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(attachmentService.upload(principal, postId, file));
  }

  @GetMapping("/api/attachments/{attachmentId}")
  public ResponseEntity<Resource> download(
      @AuthPrincipal Principal principal, @PathVariable Long attachmentId) {
    PostAttachment attachment = attachmentService.download(principal, attachmentId);
    ContentDisposition disposition =
        ContentDisposition.attachment()
            .filename(attachment.getFileName(), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .contentType(MediaType.parseMediaType(attachment.getContentType()))
        .contentLength(attachment.getSizeBytes())
        .body(new ByteArrayResource(attachment.getData()));
  }
}
