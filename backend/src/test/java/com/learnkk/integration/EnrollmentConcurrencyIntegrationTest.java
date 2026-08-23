package com.learnkk.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.enrollment.domain.EnrollmentStatus;
import com.learnkk.enrollment.repository.EnrollmentRepository;
import com.learnkk.enrollment.service.EnrollmentService;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ConflictException;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.security.Principal;
import com.learnkk.meeting.entity.Meeting;
import com.learnkk.meeting.repository.MeetingRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The load-bearing invariant (BR-U4-1): no overbooking. Under a capacity-1 meeting with N mentees
 * applying in parallel, exactly one enrollment ends APPLIED and the rest fail with ENROLLMENT_FULL.
 * Concurrent double-apply by one mentee yields exactly one APPLIED.
 */
class EnrollmentConcurrencyIntegrationTest extends AbstractIntegrationTest {

  @Autowired private EnrollmentService enrollmentService;
  @Autowired private EnrollmentRepository enrollmentRepository;
  @Autowired private MeetingRepository meetingRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private long recruitingMeeting(long mentorId, int capacity) {
    Meeting meeting =
        new Meeting(mentorId, "정원 경합", "backend", 8, null, null, capacity, "online", null);
    meeting.setStatus(MeetingStatus.RECRUITING);
    return meetingRepository.save(meeting).getId();
  }

  private long mentee(String nickname, String employeeNo) {
    return userRepository
        .save(new User(nickname, passwordEncoder.encode("password1"), employeeNo, Role.MENTEE))
        .getId();
  }

  @Test
  void capacityOne_nParallelApplies_exactlyOneApplied() throws Exception {
    long mentorId = mentee("mentorC", "conc-mentor"); // any user id as owner ref
    long meetingId = recruitingMeeting(mentorId, 1);

    int n = 8;
    List<Long> menteeIds = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      menteeIds.add(mentee("menteeC" + i, "conc-" + i));
    }

    CyclicBarrier barrier = new CyclicBarrier(n);
    ExecutorService pool = Executors.newFixedThreadPool(n);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger full = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();

    for (Long menteeId : menteeIds) {
      Callable<Void> task =
          () -> {
            barrier.await();
            try {
              enrollmentService.apply(new Principal(menteeId, Role.MENTEE), meetingId);
              success.incrementAndGet();
            } catch (ConflictException e) {
              if (ErrorCodes.ENROLLMENT_FULL.equals(e.getCode())) {
                full.incrementAndGet();
              }
            }
            return null;
          };
      futures.add(pool.submit(task));
    }
    for (Future<?> f : futures) {
      f.get();
    }
    pool.shutdown();

    assertThat(success.get()).isEqualTo(1);
    assertThat(full.get()).isEqualTo(n - 1);
    assertThat(enrollmentRepository.countByMeetingIdAndStatus(meetingId, EnrollmentStatus.APPLIED))
        .isEqualTo(1);
  }

  @Test
  void concurrentDuplicateApply_yieldsExactlyOneApplied() throws Exception {
    long mentorId = mentee("mentorD", "dup-mentor");
    long meetingId = recruitingMeeting(mentorId, 10);
    long menteeId = mentee("menteeDup", "dup-mentee");

    int n = 5;
    CyclicBarrier barrier = new CyclicBarrier(n);
    ExecutorService pool = Executors.newFixedThreadPool(n);
    AtomicInteger success = new AtomicInteger();
    AtomicInteger duplicate = new AtomicInteger();
    List<Future<?>> futures = new ArrayList<>();

    for (int i = 0; i < n; i++) {
      Callable<Void> task =
          () -> {
            barrier.await();
            try {
              enrollmentService.apply(new Principal(menteeId, Role.MENTEE), meetingId);
              success.incrementAndGet();
            } catch (ConflictException e) {
              if (ErrorCodes.ENROLLMENT_DUPLICATE.equals(e.getCode())) {
                duplicate.incrementAndGet();
              }
            }
            return null;
          };
      futures.add(pool.submit(task));
    }
    for (Future<?> f : futures) {
      f.get();
    }
    pool.shutdown();

    assertThat(success.get()).isEqualTo(1);
    assertThat(duplicate.get()).isEqualTo(n - 1);
    assertThat(enrollmentRepository.countByMeetingIdAndStatus(meetingId, EnrollmentStatus.APPLIED))
        .isEqualTo(1);
  }
}
