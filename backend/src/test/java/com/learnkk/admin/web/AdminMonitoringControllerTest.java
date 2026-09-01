package com.learnkk.admin.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.admin.dto.MeetingMonitoringSummary;
import com.learnkk.admin.service.AdminMonitoringService;
import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.MentorCompletionStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminMonitoringController.class)
class AdminMonitoringControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private AdminMonitoringService monitoringService;

  private static PageResponse<MeetingMonitoringSummary> pageOf(MeetingMonitoringSummary... rows) {
    return new PageResponse<>(List.of(rows), 0, 20, rows.length, 1);
  }

  private static MeetingMonitoringSummary row() {
    return new MeetingMonitoringSummary(
        10L,
        "자바 스터디",
        MeetingStatus.IN_PROGRESS,
        1L,
        "멘토닉",
        5,
        4,
        2,
        0.85,
        1,
        3,
        MentorCompletionStatus.PENDING);
  }

  @Test
  void list_asAdmin_returns200WithComposedRow() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(monitoringService.listMeetings(any(), isNull(), any())).thenReturn(pageOf(row()));

    mockMvc
        .perform(get("/api/admin/monitoring/meetings").header("Authorization", "Bearer a-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(10))
        .andExpect(jsonPath("$.content[0].status").value("IN_PROGRESS"))
        .andExpect(jsonPath("$.content[0].attendanceRate").value(0.85))
        .andExpect(jsonPath("$.content[0].completedMenteeCount").value(1))
        .andExpect(jsonPath("$.content[0].mentorNickname").value("멘토닉"));
  }

  @Test
  void list_withStatusFilter_passesParsedStatus() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(monitoringService.listMeetings(any(), eq(MeetingStatus.IN_PROGRESS), any()))
        .thenReturn(pageOf(row()));

    mockMvc
        .perform(
            get("/api/admin/monitoring/meetings?status=in_progress")
                .header("Authorization", "Bearer a-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void list_unknownStatus_returns400() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));

    mockMvc
        .perform(
            get("/api/admin/monitoring/meetings?status=NOPE")
                .header("Authorization", "Bearer a-tok"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED));
  }

  @Test
  void list_asMentor_returns403() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(monitoringService.listMeetings(any(), isNull(), any()))
        .thenThrow(new ForbiddenException(ErrorCodes.MONITORING_FORBIDDEN, "관리자만 운영 현황을 조회할 수 있습니다."));

    mockMvc
        .perform(get("/api/admin/monitoring/meetings").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.MONITORING_FORBIDDEN));
  }

  @Test
  void list_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/admin/monitoring/meetings")).andExpect(status().isUnauthorized());
  }
}
