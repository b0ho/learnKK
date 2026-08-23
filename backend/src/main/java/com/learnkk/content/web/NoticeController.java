package com.learnkk.content.web;

import com.learnkk.content.dto.NoticeCreateRequest;
import com.learnkk.content.dto.NoticeResponse;
import com.learnkk.content.service.NoticeService;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Notice endpoints: post (owning mentor) / list (participants). */
@RestController
public class NoticeController {

  private final NoticeService noticeService;

  public NoticeController(NoticeService noticeService) {
    this.noticeService = noticeService;
  }

  @PostMapping("/api/meetings/{id}/notices")
  public ResponseEntity<NoticeResponse> post(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody NoticeCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(noticeService.postNotice(principal, id, request));
  }

  @GetMapping("/api/meetings/{id}/notices")
  public ResponseEntity<List<NoticeResponse>> list(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(noticeService.listNotices(principal, id));
  }
}
