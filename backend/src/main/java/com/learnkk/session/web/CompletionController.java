package com.learnkk.session.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.session.dto.MenteeCompletionResponse;
import com.learnkk.session.service.CompletionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 수료 판정·④ 확정 엔드포인트(U5, W3/W4). */
@RestController
public class CompletionController {

  private final CompletionService completionService;

  public CompletionController(CompletionService completionService) {
    this.completionService = completionService;
  }

  /** 소유 멘토/관리자: 참여 멘티 80% 수료 자동 판정 실행. */
  @PostMapping("/api/meetings/{id}/completions/compute")
  public ResponseEntity<List<MenteeCompletionResponse>> compute(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(completionService.computeCompletion(principal, id));
  }

  /** 소유 멘토/관리자: 수료 판정 결과 조회. */
  @GetMapping("/api/meetings/{id}/completions")
  public ResponseEntity<List<MenteeCompletionResponse>> list(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(completionService.getCompletions(principal, id));
  }

  /** 관리자 ④: 개별 멘티 수료 확정. */
  @PostMapping("/api/admin/meetings/{id}/completions/{menteeId}/approve")
  public ResponseEntity<MenteeCompletionResponse> approve(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @PathVariable Long menteeId) {
    return ResponseEntity.ok(
        completionService.approveMenteeCompletion(principal, id, menteeId));
  }
}
