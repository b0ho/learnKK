package com.learnkk.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.content.dto.NoticeCreateRequest;
import com.learnkk.content.dto.NoticeResponse;
import com.learnkk.content.entity.Notice;
import com.learnkk.content.repository.NoticeRepository;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

  @Mock private NoticeRepository noticeRepository;
  @Mock private ContentAccessService accessService;

  @InjectMocks private NoticeService noticeService;

  private final Principal mentor = new Principal(1L, Role.MENTOR);

  @Test
  void postNotice_owningMentor_ok() {
    when(noticeRepository.save(any(Notice.class))).thenAnswer(inv -> inv.getArgument(0));

    NoticeResponse response =
        noticeService.postNotice(mentor, 10L, new NoticeCreateRequest("공지입니다."));

    assertThat(response.body()).isEqualTo("공지입니다.");
    assertThat(response.meetingId()).isEqualTo(10L);
    verify(accessService).assertOwningMentor(mentor, 10L);
  }

  @Test
  void listNotices_participant_returnsNotices() {
    when(noticeRepository.findByMeetingIdOrderByCreatedAtDesc(10L))
        .thenReturn(List.of(new Notice(10L, 1L, "공지")));

    List<NoticeResponse> notices = noticeService.listNotices(mentor, 10L);

    assertThat(notices).hasSize(1);
    verify(accessService).assertParticipant(mentor, 10L);
  }
}
