package com.learnkk.admin.web;

import com.learnkk.admin.dto.ApprovalQueues;
import com.learnkk.admin.dto.MeetingMonitorRow;
import com.learnkk.admin.service.AdminQueryService;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * U9 Admin/Monitoring 조회 엔드포인트(관리자 전용, read-only). 승인 액션은 {@code /api/admin/meetings}(U3) 와
 * 수료 확정(U5)이 담당하며, 여기서는 큐·현황 조회만 제공한다.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminQueryController {

  private final AdminQueryService adminQueryService;

  public AdminQueryController(AdminQueryService adminQueryService) {
    this.adminQueryService = adminQueryService;
  }

  /** US-9.1: 승인 큐 집계(5개 큐). */
  @GetMapping("/queues")
  public ResponseEntity<ApprovalQueues> queues(@AuthPrincipal Principal principal) {
    return ResponseEntity.ok(adminQueryService.getApprovalQueues(principal));
  }

  /** US-9.2: 운영 현황 모니터링(모임별 현황). */
  @GetMapping("/monitoring")
  public ResponseEntity<List<MeetingMonitorRow>> monitoring(@AuthPrincipal Principal principal) {
    return ResponseEntity.ok(adminQueryService.getMonitoring(principal));
  }
}
