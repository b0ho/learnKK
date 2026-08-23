package com.learnkk.survey.web;

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
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.Principal;
import com.learnkk.survey.dto.SurveyAnswerResponse;
import com.learnkk.survey.service.PreSurveyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SurveyController.class)
class SurveyControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthService authService;
  @MockBean private PreSurveyService preSurveyService;

  private static final String BODY = "{\"answers\":[{\"questionId\":100,\"answerText\":\"답변\"}]}";

  @Test
  void submit_asMentee_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(preSurveyService.submitAnswers(any(), eq(10L), any()))
        .thenReturn(List.of(new SurveyAnswerResponse(100L, "답변")));

    mockMvc
        .perform(
            post("/api/meetings/10/survey-answers")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].questionId").value(100));
  }

  @Test
  void submit_noToken_returns401() throws Exception {
    mockMvc
        .perform(
            post("/api/meetings/10/survey-answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void submit_beforeStart_returns409() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(preSurveyService.submitAnswers(any(), eq(10L), any()))
        .thenThrow(new ConflictException(ErrorCodes.PRESURVEY_NOT_OPEN, "시작 후 응답"));

    mockMvc
        .perform(
            post("/api/meetings/10/survey-answers")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.PRESURVEY_NOT_OPEN));
  }

  @Test
  void submit_requiredMissing_returns400() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(preSurveyService.submitAnswers(any(), eq(10L), any()))
        .thenThrow(new ValidationException(ErrorCodes.PRESURVEY_REQUIRED_MISSING, "필수 누락"));

    mockMvc
        .perform(
            post("/api/meetings/10/survey-answers")
                .header("Authorization", "Bearer me-tok")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.PRESURVEY_REQUIRED_MISSING));
  }

  @Test
  void mine_asMentee_returns200() throws Exception {
    when(authService.validateSession("me-tok")).thenReturn(new Principal(2L, Role.MENTEE));
    when(preSurveyService.getAnswers(any(), eq(10L), eq(2L)))
        .thenReturn(List.of(new SurveyAnswerResponse(100L, "답변")));

    mockMvc
        .perform(
            get("/api/meetings/10/survey-answers/mine").header("Authorization", "Bearer me-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].answerText").value("답변"));
  }

  @Test
  void byMentee_owningMentor_returns200() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(1L, Role.MENTOR));
    when(preSurveyService.getAnswers(any(), eq(10L), eq(2L)))
        .thenReturn(List.of(new SurveyAnswerResponse(100L, "답변")));

    mockMvc
        .perform(
            get("/api/meetings/10/mentees/2/survey-answers")
                .header("Authorization", "Bearer m-tok"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].questionId").value(100));
  }

  @Test
  void byMentee_otherMentor_returns403() throws Exception {
    when(authService.validateSession("m-tok")).thenReturn(new Principal(5L, Role.MENTOR));
    when(preSurveyService.getAnswers(any(), eq(10L), eq(2L)))
        .thenThrow(new ForbiddenException(ErrorCodes.PRESURVEY_FORBIDDEN, "권한 없음"));

    mockMvc
        .perform(
            get("/api/meetings/10/mentees/2/survey-answers")
                .header("Authorization", "Bearer m-tok"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value(ErrorCodes.PRESURVEY_FORBIDDEN));
  }

  @Test
  void byMentee_noToken_returns401() throws Exception {
    mockMvc
        .perform(get("/api/meetings/10/mentees/2/survey-answers"))
        .andExpect(status().isUnauthorized());
  }
}
