package com.learnkk.content.service;

import com.learnkk.content.dto.NoticeCreateRequest;
import com.learnkk.content.dto.NoticeResponse;
import com.learnkk.content.entity.Notice;
import com.learnkk.content.repository.NoticeRepository;
import com.learnkk.kernel.security.Principal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notice workflow (C5, U6, US-4.3). The owning mentor posts notices (403 otherwise); participants
 * read them (403 otherwise). Authorization is delegated to {@link ContentAccessService}.
 */
@Service
public class NoticeService {

  private final NoticeRepository noticeRepository;
  private final ContentAccessService accessService;

  public NoticeService(NoticeRepository noticeRepository, ContentAccessService accessService) {
    this.noticeRepository = noticeRepository;
    this.accessService = accessService;
  }

  /** Post a notice (US-4.3). Owning mentor only (403). */
  @Transactional
  public NoticeResponse postNotice(Principal principal, Long meetingId, NoticeCreateRequest req) {
    accessService.assertOwningMentor(principal, meetingId);
    Notice saved = noticeRepository.save(new Notice(meetingId, principal.userId(), req.body()));
    return NoticeResponse.from(saved);
  }

  /** List a meeting's notices, newest first (US-4.3). Participants only (403). */
  @Transactional(readOnly = true)
  public List<NoticeResponse> listNotices(Principal principal, Long meetingId) {
    accessService.assertParticipant(principal, meetingId);
    return noticeRepository.findByMeetingIdOrderByCreatedAtDesc(meetingId).stream()
        .map(NoticeResponse::from)
        .toList();
  }
}
