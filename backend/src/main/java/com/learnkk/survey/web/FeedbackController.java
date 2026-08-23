package com.learnkk.survey.web;

import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.survey.dto.FeedbackRequest;
import com.learnkk.survey.dto.FeedbackResponse;
import com.learnkk.survey.service.FeedbackService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Course feedback endpoints (U8). */
@RestController
public class FeedbackController {

  private final FeedbackService feedbackService;

  public FeedbackController(FeedbackService feedbackService) {
    this.feedbackService = feedbackService;
  }

  @PostMapping("/api/meetings/{id}/feedback")
  public ResponseEntity<FeedbackResponse> submit(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody FeedbackRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(feedbackService.submitFeedback(principal, id, request.content()));
  }

  @GetMapping("/api/meetings/{id}/feedback")
  public ResponseEntity<List<FeedbackResponse>> list(
      @AuthPrincipal Principal principal, @PathVariable Long id) {
    return ResponseEntity.ok(feedbackService.listFeedback(principal, id));
  }
}
