package com.learnkk.survey.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.survey.dto.SurveyAnswerRequest;
import com.learnkk.survey.dto.SurveyAnswerResponse;
import com.learnkk.survey.service.PreSurveyService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Pre-application survey answer endpoints (U8). */
@RestController
public class SurveyController {

  private final PreSurveyService preSurveyService;

  public SurveyController(PreSurveyService preSurveyService) {
    this.preSurveyService = preSurveyService;
  }

  @PostMapping("/api/meetings/{id}/survey-answers")
  public ResponseEntity<List<SurveyAnswerResponse>> submit(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody SurveyAnswerRequest request) {
    return ResponseEntity.ok(preSurveyService.submitAnswers(principal, id, request));
  }

  @GetMapping("/api/meetings/{id}/survey-answers/mine")
  public ResponseEntity<List<SurveyAnswerResponse>> mine(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(preSurveyService.getAnswers(principal, id, principal.userId()));
  }

  @GetMapping("/api/meetings/{id}/mentees/{menteeId}/survey-answers")
  public ResponseEntity<List<SurveyAnswerResponse>> byMentee(
      @AuthPrincipal Principal principal, @PathVariable Long id, @PathVariable Long menteeId) {
    return ResponseEntity.ok(preSurveyService.getAnswers(principal, id, menteeId));
  }
}
