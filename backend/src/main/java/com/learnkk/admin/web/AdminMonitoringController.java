package com.learnkk.admin.web;

import com.learnkk.admin.dto.MeetingMonitoringSummary;
import com.learnkk.admin.service.AdminMonitoringService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageRequestFactory;
import com.learnkk.kernel.web.PageResponse;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin operational-monitoring endpoint (US-9.2, U9): per-meeting status, session-based attendance
 * rate and completion progress. Read-only composition — approval actions stay on
 * {@code /api/admin/meetings} (U3/U5).
 */
@RestController
@RequestMapping("/api/admin/monitoring")
public class AdminMonitoringController {

  private static final Set<String> SORTABLE = Set.of("id", "createdAt", "title");

  private final AdminMonitoringService monitoringService;

  public AdminMonitoringController(AdminMonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  /** US-9.2: 전체 모임 운영 현황(상태 필터는 선택). 관리자 전용. */
  @GetMapping("/meetings")
  public ResponseEntity<PageResponse<MeetingMonitoringSummary>> listMeetings(
      @AuthPrincipal Principal principal,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort) {
    MeetingStatus parsed = null;
    if (StringUtils.hasText(status)) {
      try {
        parsed = MeetingStatus.valueOf(status.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new ValidationException(
            ErrorCodes.VALIDATION_FAILED, "지원하지 않는 status 값입니다: " + status);
      }
    }
    Pageable pageable = PageRequestFactory.of(page, size, sort, SORTABLE);
    return ResponseEntity.ok(monitoringService.listMeetings(principal, parsed, pageable));
  }
}
