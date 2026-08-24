package com.learnkk.session.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.session.dto.CreateSessionRequest;
import com.learnkk.session.dto.SessionResponse;
import com.learnkk.session.dto.UpdateSessionRequest;
import com.learnkk.session.service.SessionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 세션 일정 관리 엔드포인트(U5, W1). */
@RestController
public class SessionController {

  private final SessionService sessionService;

  public SessionController(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @PostMapping("/api/meetings/{id}/sessions")
  public ResponseEntity<SessionResponse> addSession(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody CreateSessionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(sessionService.addSession(principal, id, request));
  }

  @PutMapping("/api/sessions/{id}")
  public ResponseEntity<SessionResponse> updateSession(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody UpdateSessionRequest request) {
    return ResponseEntity.ok(sessionService.updateSession(principal, id, request));
  }

  @GetMapping("/api/meetings/{id}/sessions")
  public ResponseEntity<List<SessionResponse>> listSessions(@PathVariable Long id) {
    return ResponseEntity.ok(sessionService.listSessions(id));
  }

  /** 세션 삭제(FR-7, 소유 멘토). 출석 기록은 CASCADE 로 함께 삭제. */
  @DeleteMapping("/api/sessions/{id}")
  public ResponseEntity<Void> deleteSession(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    sessionService.deleteSession(principal, id);
    return ResponseEntity.noContent().build();
  }

  /** 세션 완료 처리(FR-8, 소유 멘토). */
  @PostMapping("/api/sessions/{id}/complete")
  public ResponseEntity<SessionResponse> completeSession(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(sessionService.completeSession(principal, id));
  }
}
