package com.learnkk.enrollment.repository;

import com.learnkk.enrollment.domain.EnrollmentStatus;
import com.learnkk.enrollment.entity.Enrollment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  int countByMeetingIdAndStatus(Long meetingId, EnrollmentStatus status);

  Optional<Enrollment> findByMeetingIdAndMenteeId(Long meetingId, Long menteeId);

  List<Enrollment> findByMeetingIdAndStatus(Long meetingId, EnrollmentStatus status);

  List<Enrollment> findByMenteeIdOrderByAppliedAtDesc(Long menteeId);

  /**
   * Acquires a transaction-scoped PostgreSQL advisory lock keyed by the meeting id (BR-U4-1). The
   * lock is held until the surrounding transaction commits, so the next {@code apply} for the same
   * meeting observes the committed insert before it counts — making count-then-insert race-free
   * without ever locking the U3-owned meetings row.
   */
  @Query(value = "SELECT pg_advisory_xact_lock(:key)", nativeQuery = true)
  void lockMeeting(@Param("key") long key);
}
