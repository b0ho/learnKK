package com.learnkk.meeting.web;

import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ValidationException;
import com.learnkk.kernel.security.AuthPrincipal;
import com.learnkk.kernel.security.Principal;
import com.learnkk.kernel.web.PageRequestFactory;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.dto.MeetingCreateRequest;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.MeetingSummary;
import com.learnkk.meeting.dto.SurveyQuestionDto;
import com.learnkk.meeting.service.MeetingService;
import com.learnkk.meeting.service.SurveyTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public/mentor meeting endpoints. */
@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

  private static final Set<String> SORTABLE = Set.of("id", "createdAt", "title");

  private final MeetingService meetingService;
  private final SurveyTemplateService surveyTemplateService;

  public MeetingController(
      MeetingService meetingService, SurveyTemplateService surveyTemplateService) {
    this.meetingService = meetingService;
    this.surveyTemplateService = surveyTemplateService;
  }

  @PostMapping
  public ResponseEntity<MeetingResponse> create(
      @AuthPrincipal Principal principal, @Valid @RequestBody MeetingCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(meetingService.createMeeting(principal, request));
  }

  @GetMapping("/{id}")
  public ResponseEntity<MeetingResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(meetingService.getMeeting(id));
  }

  @GetMapping
  public ResponseEntity<PageResponse<MeetingSummary>> list(
      @RequestParam(defaultValue = "recruiting") String status,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort) {
    if (!"recruiting".equalsIgnoreCase(status)) {
      // Only the public recruiting listing is exposed here; the full status-filtered queue is U9
      // (Bolt 8). The mentor's own meetings are served by the dedicated /mine route.
      throw new ValidationException(ErrorCodes.VALIDATION_FAILED, "지원하지 않는 status 값입니다: " + status);
    }
    Pageable pageable = PageRequestFactory.of(page, size, sort, SORTABLE);
    return ResponseEntity.ok(meetingService.listRecruiting(pageable));
  }

  /** Mentor operations hub: the caller's own meetings across every status (US-2.3). */
  @GetMapping("/mine")
  public ResponseEntity<PageResponse<MeetingSummary>> listMine(
      @AuthPrincipal Principal principal,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(required = false) String sort) {
    Pageable pageable = PageRequestFactory.of(page, size, sort, SORTABLE);
    return ResponseEntity.ok(meetingService.listMyMeetings(principal, pageable));
  }

  @PutMapping("/{id}/questions")
  public ResponseEntity<List<SurveyQuestionDto>> putQuestions(
      @AuthPrincipal Principal principal,
      @PathVariable Long id,
      @Valid @RequestBody List<SurveyQuestionDto> questions) {
    return ResponseEntity.ok(
        surveyTemplateService.upsertQuestions(principal.userId(), id, questions));
  }

  @GetMapping("/{id}/questions")
  public ResponseEntity<List<SurveyQuestionDto>> getQuestions(@PathVariable Long id) {
    return ResponseEntity.ok(surveyTemplateService.getQuestions(id));
  }
}
