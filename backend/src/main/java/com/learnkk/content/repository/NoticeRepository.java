package com.learnkk.content.repository;

import com.learnkk.content.entity.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

  /** Notices of a meeting, newest first. */
  List<Notice> findByMeetingIdOrderByCreatedAtDesc(Long meetingId);
}
