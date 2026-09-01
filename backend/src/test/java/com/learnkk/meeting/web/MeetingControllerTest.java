package com.learnkk.meeting.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.learnkk.auth.service.AuthService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.MeetingSummary;
import com.learnkk.meeting.service.MeetingApprovalService;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.meeting.service.SurveyTemplateService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({MeetingController.class, MeetingApprovalController.class})
class MeetingControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private MeetingService meetingService;
  @MockBean private SurveyTemplateService surveyTemplateService;
  @MockBean private MeetingApprovalService approvalService;

  private MeetingResponse meeting(MeetingStatus status) {
    return new MeetingResponse(
        10L, 1L, "Spring", "backend", 8, null, null, 5, "online", "intro", status, null, null);
  }

  @Test
  void createMeeting_asMentor_returns201() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(meetingService.createMeeting(any(), any()))
        .thenReturn(meeting(MeetingStatus.PENDING_APPROVAL));

    mockMvc
        .perform(
            post("/api/meetings")
                .header("Authorization", "Bearer m-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Spring","topic":"backend","weeks":8,"capacity":5}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
        .andExpect(jsonPath("$.title").value("Spring"));
  }

  @Test
  void createMeeting_noToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/meetings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Spring","weeks":8,"capacity":5}
                    """))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createMeeting_asMentee_returns403() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(meetingService.createMeeting(any(), any()))
        .thenThrow(new ForbiddenException(ErrorCodes.MEETING_FORBIDDEN, "멘토만 모임을 개설할 수 있습니다."));

    mockMvc
        .perform(
            post("/api/meetings")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"title":"Spring","weeks":8,"capacity":5}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.MEETING_FORBIDDEN));
  }

  @Test
  void listRecruiting_isPublic_returns200() throws Exception {
    PageResponse<MeetingSummary> page =
        new PageResponse<>(
            List.of(new MeetingSummary(10L, "Spring", "backend", 8, 5, MeetingStatus.RECRUITING, null)),
            0,
            20,
            1,
            1);
    when(meetingService.listRecruiting(any())).thenReturn(page);

    mockMvc
        .perform(get("/api/meetings").param("status", "recruiting"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].status").value("RECRUITING"));
  }

  @Test
  void getMeeting_isPublic_returns200() throws Exception {
    when(meetingService.getMeeting(10L)).thenReturn(meeting(MeetingStatus.RECRUITING));

    mockMvc
        .perform(get("/api/meetings/10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(10));
  }

  @Test
  void approve_asAdmin_returns200() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(approvalService.approveCreation(any(), any()))
        .thenReturn(meeting(MeetingStatus.RECRUITING));

    mockMvc
        .perform(post("/api/admin/meetings/10/approve").header("Authorization", "Bearer a-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECRUITING"));
  }

  @Test
  void approve_noToken_returns401() throws Exception {
    mockMvc.perform(post("/api/admin/meetings/10/approve")).andExpect(status().isUnauthorized());
  }

  @Test
  void confirmRecruitment_asAdmin_returns200() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(approvalService.confirmRecruitment(
            any(), any(), org.mockito.ArgumentMatchers.eq(true), any()))
        .thenReturn(meeting(MeetingStatus.READY_TO_START));

    mockMvc
        .perform(
            post("/api/admin/meetings/10/confirm-recruitment")
                .header("Authorization", "Bearer a-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"proceed":true}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY_TO_START"));
  }

  @Test
  void confirmRecruitment_missingProceed_returns400() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));

    mockMvc
        .perform(
            post("/api/admin/meetings/10/confirm-recruitment")
                .header("Authorization", "Bearer a-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"reason":"x"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED));
  }

  @Test
  void approveStart_asAdmin_returns200() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(approvalService.approveStart(any(), any())).thenReturn(meeting(MeetingStatus.IN_PROGRESS));

    mockMvc
        .perform(
            post("/api/admin/meetings/10/approve-start").header("Authorization", "Bearer a-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
  }

  @Test
  void complete_asAdmin_returns200() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));
    when(approvalService.completeMeeting(any(), any()))
        .thenReturn(meeting(MeetingStatus.COMPLETED));

    mockMvc
        .perform(post("/api/admin/meetings/10/complete").header("Authorization", "Bearer a-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void complete_noToken_returns401() throws Exception {
    mockMvc.perform(post("/api/admin/meetings/10/complete")).andExpect(status().isUnauthorized());
  }

  @Test
  void reject_missingReason_returns400() throws Exception {
    when(authService.validateSession("a-tok")).thenReturn(new Principal(9L, Role.ADMIN));

    mockMvc
        .perform(
            post("/api/admin/meetings/10/reject")
                .header("Authorization", "Bearer a-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"reason":""}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.VALIDATION_FAILED));
  }

  @Test
  void listMine_asMentor_returns200() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    PageResponse<MeetingSummary> page =
        new PageResponse<>(
            List.of(new MeetingSummary(10L, "내 모임", "backend", 8, 5, MeetingStatus.READY_TO_START, null)),
            0,
            20,
            1,
            1);
    when(meetingService.listMyMeetings(any(), any())).thenReturn(page);

    mockMvc
        .perform(get("/api/meetings/mine").header("Authorization", "Bearer m-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].status").value("READY_TO_START"));
  }

  @Test
  void listMine_noToken_returns401() throws Exception {
    mockMvc.perform(get("/api/meetings/mine")).andExpect(status().isUnauthorized());
  }
}
