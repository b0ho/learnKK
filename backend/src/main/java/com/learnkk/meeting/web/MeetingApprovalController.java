package com.learnkk.meeting.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.RejectRequest;
import com.learnkk.meeting.service.MeetingApprovalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin meeting approval endpoints (creation gate). */
@RestController
@RequestMapping("/api/admin/meetings")
public class MeetingApprovalController {

  private final MeetingApprovalService approvalService;

  public MeetingApprovalController(MeetingApprovalService approvalService) {
    this.approvalService = approvalService;
  }

  @PostMapping("/{id}/approve")
  public ResponseEntity<MeetingResponse> approve(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(approvalService.approveCreation(principal, id));
  }

  @PostMapping("/{id}/reject")
  public ResponseEntity<MeetingResponse> reject(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @RequestBody(required = false) RejectRequest request) {
    String reason = request == null ? null : request.reason();
    return ResponseEntity.ok(approvalService.rejectCreation(principal, id, reason));
  }
}
