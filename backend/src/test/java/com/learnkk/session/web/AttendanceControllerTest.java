package com.learnkk.session.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.security.Principal;
import com.learnkk.session.dto.AttendanceResponse;
import com.learnkk.session.dto.AttendanceSummaryResponse;
import com.learnkk.session.service.AttendanceService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private AttendanceService attendanceService;

  @Test
  void checkIn_withinWindow_returns201() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(attendanceService.checkIn(any(), eq(5L)))
        .thenReturn(new AttendanceResponse(5L, 2L, OffsetDateTime.parse("2026-01-01T10:05Z")));

    mockMvc
        .perform(post("/api/sessions/5/attendance").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sessionId").value(5))
        .andExpect(jsonPath("$.menteeId").value(2));
  }

  @Test
  void checkIn_windowClosed_returns409() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(attendanceService.checkIn(any(), eq(5L)))
        .thenThrow(new ConflictException(ErrorCodes.ATTENDANCE_WINDOW_CLOSED, "출석 시간이 아닙니다."));

    mockMvc
        .perform(post("/api/sessions/5/attendance").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.ATTENDANCE_WINDOW_CLOSED));
  }

  @Test
  void checkIn_noToken_returns401() throws Exception {
    mockMvc.perform(post("/api/sessions/5/attendance")).andExpect(status().isUnauthorized());
  }

  @Test
  void getMyAttendance_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(attendanceService.getMyAttendance(any(), eq(10L)))
        .thenReturn(AttendanceSummaryResponse.of(3, 4));

    mockMvc
        .perform(get("/api/meetings/10/my-attendance").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attended").value(3))
        .andExpect(jsonPath("$.totalScheduled").value(4))
        .andExpect(jsonPath("$.rate").value(0.75));
  }

  @Test
  void getMyAttendance_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/meetings/10/my-attendance")).andExpect(status().isUnauthorized());
  }
}
