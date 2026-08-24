package com.learnkk.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.kernel.domain.Role;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * U5 세션·출석·수료 관통 통합 테스트: 세션 생성 → 시간창 내 checkIn → 출석율 → computeCompletion → ④ approve.
 * 창 밖 409, 멱등도 검증한다. Testcontainers postgres 필요(미가용 환경에서는 스킵될 수 있음 — 코드 결함 아님).
 */
class SessionAttendanceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void sessionAttendanceCompletion_endToEnd() throws Exception {
    String mentorToken = signupAndLogin("mentorS", "sess-mentor", "MENTOR");
    String adminToken = createAdminAndLogin("adminS", "sess-admin");
    String menteeToken = signupAndLogin("menteeS", "sess-mentee", "MENTEE");

    long meetingId = createMeeting(mentorToken);
    approve(adminToken, meetingId, "approve", "RECRUITING");

    // 멘티 신청.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/enrollments")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isCreated());

    // RECRUITING → READY_TO_START → IN_PROGRESS.
    confirmRecruitment(adminToken, meetingId);
    approve(adminToken, meetingId, "approve-start", "IN_PROGRESS");

    // 멘토가 세션 추가(지금 시각 기준, 시간창 안).
    String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    MvcResult sessionResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/sessions")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            "{\"week\":1,\"scheduledAt\":\"%s\",\"checkInWindowMinutes\":120}",
                            now)))
            .andExpect(status().isCreated())
            .andReturn();
    long sessionId =
        objectMapper.readTree(sessionResult.getResponse().getContentAsString()).get("id").asLong();

    // 시간창 내 출석 → 201.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/sessions/" + sessionId + "/attendance")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sessionId").value((int) sessionId));

    // 멱등: 재요청도 성공(중복 생성 없음).
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/sessions/" + sessionId + "/attendance")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isCreated());

    // 출석율: a=1, S=1 → rate 1.0.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/my-attendance")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.attended").value(1))
        .andExpect(jsonPath("$.totalScheduled").value(1))
        .andExpect(jsonPath("$.rate").value(1.0));

    // 수료 판정: 1/1 = 100% ≥ 80% → 후보.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/completions/compute")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("COMPLETION_CANDIDATE"));

    // ④ 관리자 확정.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    "/api/admin/meetings/" + meetingId + "/completions/" + menteeUserId() + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("COMPLETED"));
  }

  @Test
  void checkInOutsideWindow_returns409() throws Exception {
    String mentorToken = signupAndLogin("mentorW", "win-mentor", "MENTOR");
    String adminToken = createAdminAndLogin("adminW", "win-admin");
    String menteeToken = signupAndLogin("menteeW", "win-mentee", "MENTEE");

    long meetingId = createMeeting(mentorToken);
    approve(adminToken, meetingId, "approve", "RECRUITING");
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/enrollments")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isCreated());
    confirmRecruitment(adminToken, meetingId);
    approve(adminToken, meetingId, "approve-start", "IN_PROGRESS");

    // 미래 세션(시간창 시작 전).
    String future =
        OffsetDateTime.now().plusDays(3).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    MvcResult sessionResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/sessions")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format("{\"week\":1,\"scheduledAt\":\"%s\"}", future)))
            .andExpect(status().isCreated())
            .andReturn();
    long sessionId =
        objectMapper.readTree(sessionResult.getResponse().getContentAsString()).get("id").asLong();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/sessions/" + sessionId + "/attendance")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ATTENDANCE_WINDOW_CLOSED"));
  }

  private long menteeUserId() {
    return userRepository.findByNickname("menteeS").orElseThrow().getId();
  }

  private void approve(String adminToken, long meetingId, String action, String expectedStatus)
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/" + action)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(expectedStatus));
  }

  private void confirmRecruitment(String adminToken, long meetingId) throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    "/api/admin/meetings/" + meetingId + "/confirm-recruitment")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"proceed\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY_TO_START"));
  }

  private long createMeeting(String mentorToken) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"세션 테스트\",\"topic\":\"backend\",\"weeks\":8,"
                            + "\"capacity\":5,\"format\":\"online\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
  }

  private String signupAndLogin(String nickname, String employeeNo, String role) throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    String.format(
                        "{\"nickname\":\"%s\",\"password\":\"password1\",\"employeeNo\":\"%s\",\"role\":\"%s\"}",
                        nickname, employeeNo, role)))
        .andExpect(status().isCreated());
    return login(nickname);
  }

  private String createAdminAndLogin(String nickname, String employeeNo) throws Exception {
    userRepository.save(
        new User(
            nickname, passwordEncoder.encode("password1"), employeeNo.toUpperCase(), Role.ADMIN));
    return login(nickname);
  }

  private String login(String nickname) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            "{\"nickname\":\"%s\",\"password\":\"password1\"}", nickname)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    return node.get("token").asText();
  }
}
