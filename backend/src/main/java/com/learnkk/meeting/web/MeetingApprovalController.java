package com.learnkk.meeting.web;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageRequestFactory;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.dto.ConfirmRecruitmentRequest;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.MeetingSummary;
import com.learnkk.meeting.dto.MentorCompletionRequest;
import com.learnkk.meeting.dto.RejectRequest;
import com.learnkk.meeting.service.MeetingApprovalService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  /** FR-5: 승인 되돌리기(직전 상태로 역전이, 관리자 전용). */
  @PostMapping("/{id}/revert")
  public ResponseEntity<MeetingResponse> revert(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(approvalService.revert(principal, id));
  }

  /** FR-7: 멘토 수료 판정(수료/미수료, 관리자 판단만, 관리자 전용). */
  @PostMapping("/{id}/mentor-completion")
  public ResponseEntity<MeetingResponse> judgeMentorCompletion(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody MentorCompletionRequest request) {
    return ResponseEntity.ok(
        approvalService.judgeMentorCompletion(principal, id, request.status()));
  }

  /** FR-2/FR-3: 상태별 모임 목록(관리자 승인 큐). */
  @GetMapping
  public ResponseEntity<PageResponse<MeetingSummary>> listByStatus(
      @AuthPrincipal Principal principal,
      @RequestParam String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort) {
    MeetingStatus parsed;
    try {
      parsed = MeetingStatus.valueOf(status.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ValidationException(ErrorCodes.VALIDATION_FAILED, "지원하지 않는 status 값입니다: " + status);
    }
    Pageable pageable = PageRequestFactory.of(page, size, sort, SORTABLE);
    return ResponseEntity.ok(approvalService.listByStatus(principal, parsed, pageable));
  }

  private static final Set<String> SORTABLE = Set.of("id", "createdAt", "title");
}
