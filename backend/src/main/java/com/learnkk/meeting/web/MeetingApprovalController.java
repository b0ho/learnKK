package com.learnkk.meeting.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.dto.ConfirmRecruitmentRequest;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.RejectRequest;
import com.learnkk.meeting.service.MeetingApprovalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin meeting lifecycle endpoints: the creation gate (approve/reject) plus the Bolt 2 recruitment
 * and progression transitions (confirm-recruitment / approve-start / complete).
 */
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
      @Valid @RequestBody RejectRequest request) {
    return ResponseEntity.ok(approvalService.rejectCreation(principal, id, request.reason()));
  }

  /** T3/T4: confirm recruitment (proceed) or cancel (reason required). */
  @PostMapping("/{id}/confirm-recruitment")
  public ResponseEntity<MeetingResponse> confirmRecruitment(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody ConfirmRecruitmentRequest request) {
    return ResponseEntity.ok(
        approvalService.confirmRecruitment(principal, id, request.proceed(), request.reason()));
  }

  /** T5: start the meeting (READY_TO_START -&gt; IN_PROGRESS). */
  @PostMapping("/{id}/approve-start")
  public ResponseEntity<MeetingResponse> approveStart(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(approvalService.approveStart(principal, id));
  }

  /** T6: complete the meeting (IN_PROGRESS -&gt; COMPLETED). */
  @PostMapping("/{id}/complete")
  public ResponseEntity<MeetingResponse> complete(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(approvalService.completeMeeting(principal, id));
  }
}
