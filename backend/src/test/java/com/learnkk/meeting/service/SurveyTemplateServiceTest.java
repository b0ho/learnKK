package com.learnkk.meeting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.meeting.dto.SurveyQuestionDto;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.entity.SurveyQuestion;
import com.learnkk.meeting.repository.MeetingRepository;
import com.learnkk.meeting.repository.SurveyQuestionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SurveyTemplateServiceTest {

  @Mock private MeetingRepository meetingRepository;
  @Mock private SurveyQuestionRepository questionRepository;

  @InjectMocks private SurveyTemplateService service;

  private Meeting meeting(Long mentorId, MeetingStatus status) {
    Meeting m = new Meeting(mentorId, "t", null, 4, null, null, 5, null, null);
    m.setStatus(status);
    return m;
  }

  @Test
  void upsert_happyPath_replacesQuestions() {
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(meeting(1L, MeetingStatus.RECRUITING)));
    when(questionRepository.findByMeetingIdOrderByOrderNoAsc(1L))
        .thenReturn(List.of(new SurveyQuestion(1L, 1, "질문", "TEXT", List.of(), true)));

    List<SurveyQuestionDto> result =
        service.upsertQuestions(
            1L, 1L, List.of(new SurveyQuestionDto(1, "질문", "TEXT", List.of(), true)));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).text()).isEqualTo("질문");
    verify(questionRepository).deleteByMeetingId(1L);
    verify(questionRepository).saveAll(any());
  }

  @Test
  void upsert_notOwner_forbidden403() {
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(meeting(1L, MeetingStatus.RECRUITING)));

    assertThatThrownBy(() -> service.upsertQuestions(2L, 1L, List.of()))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_FORBIDDEN);
    verify(questionRepository, never()).deleteByMeetingId(any());
  }

  @Test
  void upsert_lockedAfterInProgress_conflict409() {
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(meeting(1L, MeetingStatus.IN_PROGRESS)));

    assertThatThrownBy(
            () ->
                service.upsertQuestions(
                    1L, 1L, List.of(new SurveyQuestionDto(1, "q", "TEXT", List.of(), true))))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_QUESTIONS_LOCKED);
  }

  @Test
  void upsert_lockedAfterCompleted_conflict409() {
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(meeting(1L, MeetingStatus.COMPLETED)));

    assertThatThrownBy(
            () ->
                service.upsertQuestions(
                    1L, 1L, List.of(new SurveyQuestionDto(1, "q", "TEXT", List.of(), true))))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_QUESTIONS_LOCKED);
  }

  @Test
  void upsert_lockedAfterCancelled_conflict409() {
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(meeting(1L, MeetingStatus.CANCELLED)));

    assertThatThrownBy(
            () ->
                service.upsertQuestions(
                    1L, 1L, List.of(new SurveyQuestionDto(1, "q", "TEXT", List.of(), true))))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_QUESTIONS_LOCKED);
  }

  @Test
  void upsert_editableWhenReadyToStart_succeeds() {
    // BR-U3-7: questions remain editable up to (but not including) IN_PROGRESS.
    when(meetingRepository.findById(1L))
        .thenReturn(Optional.of(meeting(1L, MeetingStatus.READY_TO_START)));
    when(questionRepository.findByMeetingIdOrderByOrderNoAsc(1L))
        .thenReturn(List.of(new SurveyQuestion(1L, 1, "질문", "TEXT", List.of(), true)));

    List<SurveyQuestionDto> result =
        service.upsertQuestions(
            1L, 1L, List.of(new SurveyQuestionDto(1, "질문", "TEXT", List.of(), true)));

    assertThat(result).hasSize(1);
    verify(questionRepository).deleteByMeetingId(1L);
  }

  @Test
  void upsert_meetingMissing_notFound404() {
    when(meetingRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.upsertQuestions(1L, 99L, List.of()))
        .isInstanceOf(NotFoundException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.MEETING_NOT_FOUND);
  }

  @Test
  void getQuestions_mapsEntities() {
    when(questionRepository.findByMeetingIdOrderByOrderNoAsc(1L))
        .thenReturn(
            List.of(
                new SurveyQuestion(1L, 1, "q1", "TEXT", List.of("a"), true),
                new SurveyQuestion(1L, 2, "q2", "CHOICE", List.of("x", "y"), false)));

    List<SurveyQuestionDto> result = service.getQuestions(1L);

    assertThat(result).hasSize(2);
    assertThat(result.get(1).type()).isEqualTo("CHOICE");
    assertThat(result.get(1).options()).containsExactly("x", "y");
  }
}
