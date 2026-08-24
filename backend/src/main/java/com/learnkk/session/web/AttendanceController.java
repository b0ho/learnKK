package com.learnkk.session.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.session.dto.AttendanceResponse;
import com.learnkk.session.dto.AttendanceSummaryResponse;
import com.learnkk.session.service.AttendanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** 팝업 출석·출석 현황 엔드포인트(U5, W2). */
@RestController
public class AttendanceController {

  private final AttendanceService attendanceService;

  public AttendanceController(AttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @PostMapping("/api/sessions/{id}/attendance")
  public ResponseEntity<AttendanceResponse> checkIn(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(attendanceService.checkIn(principal, id));
  }

  @GetMapping("/api/meetings/{id}/my-attendance")
  public ResponseEntity<AttendanceSummaryResponse> getMyAttendance(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(attendanceService.getMyAttendance(principal, id));
  }
}
