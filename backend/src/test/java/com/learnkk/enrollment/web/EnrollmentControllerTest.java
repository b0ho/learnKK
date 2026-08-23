package com.learnkk.enrollment.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.enrollment.domain.EnrollmentStatus;
import com.learnkk.enrollment.dto.ApplicantResponse;
import com.learnkk.enrollment.dto.EnrollmentResponse;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private EnrollmentService enrollmentService;

  @Test
  void apply_asMentee_returns201() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(enrollmentService.apply(any(), eq(10L)))
        .thenReturn(
            new EnrollmentResponse(
                1L, 10L, 2L, EnrollmentStatus.APPLIED, OffsetDateTime.parse("2026-01-01T00:00Z")));

    mockMvc
        .perform(post("/api/meetings/10/enrollments").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.status").value("APPLIED"));
  }

  @Test
  void apply_noToken_returns401() throws Exception {
    mockMvc.perform(post("/api/meetings/10/enrollments")).andExpect(status().isUnauthorized());
  }

  @Test
  void apply_capacityFull_returns409() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(enrollmentService.apply(any(), eq(10L)))
        .thenThrow(new ConflictException(ErrorCodes.ENROLLMENT_FULL, "모집 정원이 마감되었습니다."));

    mockMvc
        .perform(post("/api/meetings/10/enrollments").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.ENROLLMENT_FULL));
  }

  @Test
  void cancel_asMentee_returns204() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));

    mockMvc
        .perform(
            delete("/api/meetings/10/enrollments/mine").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isNoContent());
  }

  @Test
  void cancel_afterStart_returns409() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    doThrow(new ConflictException(ErrorCodes.ENROLLMENT_CANCEL_FORBIDDEN, "취소 불가"))
        .when(enrollmentService)
        .cancel(any(), eq(10L));

    mockMvc
        .perform(
            delete("/api/meetings/10/enrollments/mine").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.ENROLLMENT_CANCEL_FORBIDDEN));
  }

  @Test
  void cancel_noToken_returns401() throws Exception {
    mockMvc
        .perform(delete("/api/meetings/10/enrollments/mine"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listApplicants_asOwningMentor_returns200() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(enrollmentService.listApplicants(any(), eq(10L)))
        .thenReturn(
            List.of(new ApplicantResponse(2L, "멘티", OffsetDateTime.parse("2026-01-01T00:00Z"))));

    mockMvc
        .perform(get("/api/meetings/10/applicants").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].menteeId").value(2))
        .andExpect(jsonPath("$[0].nickname").value("멘티"));
  }

  @Test
  void listApplicants_nonOwner_returns403() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(5L, Role.MENTOR));
    when(enrollmentService.listApplicants(any(), eq(10L)))
        .thenThrow(new ForbiddenException(ErrorCodes.ENROLLMENT_FORBIDDEN, "권한이 없습니다."));

    mockMvc
        .perform(get("/api/meetings/10/applicants").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.ENROLLMENT_FORBIDDEN));
  }

  @Test
  void listApplicants_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/meetings/10/applicants")).andExpect(status().isUnauthorized());
  }

  @Test
  void listMine_asMentee_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(enrollmentService.listMyEnrollments(any()))
        .thenReturn(
            List.of(
                new EnrollmentResponse(
                    1L,
                    10L,
                    2L,
                    EnrollmentStatus.APPLIED,
                    OffsetDateTime.parse("2026-01-01T00:00Z"))));

    mockMvc
        .perform(get("/api/enrollments/mine").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].meetingId").value(10))
        .andExpect(jsonPath("$[0].status").value("APPLIED"));
  }

  @Test
  void listMine_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/enrollments/mine")).andExpect(status().isUnauthorized());
  }
}
