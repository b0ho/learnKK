package com.learnkk.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.kernel.domain.Role;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * End-to-end content flow (U6, Bolt 4): the owning mentor publishes a week post + attachment +
 * notice; an enrolled mentee reads and downloads them; a non-participant is refused; and the
 * validation guards (format whitelist, week range) fire — all through the full stack against a real
 * PostgreSQL.
 */
class ContentIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void postAttachNoticeDownload_endToEnd() throws Exception {
    String mentorToken = signupAndLogin("mentorC", "c-mentor", "MENTOR");
    String otherMentorToken = signupAndLogin("mentorC2", "c-mentor2", "MENTOR");
    String menteeToken = signupAndLogin("menteeC", "c-mentee", "MENTEE");
    String outsiderToken = signupAndLogin("outsiderC", "c-outsider", "MENTEE");
    String adminToken = createAdminAndLogin("adminC", "c-admin");

    // Mentor opens a meeting; admin approves -> RECRUITING; mentee enrolls -> participant.
    long meetingId = createMeeting(mentorToken, 5);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("RECRUITING"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/enrollments")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("APPLIED"));

    // Owning mentor creates a week-1 post.
    MvcResult postResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/posts")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"week\":1,\"body\":\"1주차 학습 자료입니다.\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.week").value(1))
            .andReturn();
    long postId =
        objectMapper.readTree(postResult.getResponse().getContentAsString()).get("id").asLong();

    // Owning mentor uploads a whitelisted (text/plain) attachment.
    byte[] payload = "hello learnKK attachment".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file =
        new MockMultipartFile("file", "notes.txt", "text/plain", payload);
    MvcResult uploadResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.multipart("/api/posts/" + postId + "/attachments")
                    .file(file)
                    .header("Authorization", "Bearer " + mentorToken))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.fileName").value("notes.txt"))
            .andReturn();
    long attachmentId =
        objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();

    // Participant mentee lists posts and sees the attachment metadata.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/posts")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].attachments.length()").value(1))
        .andExpect(jsonPath("$[0].attachments[0].fileName").value("notes.txt"));

    // Participant mentee downloads the attachment: bytes match, served as attachment (XSS defence).
    MvcResult download =
        mockMvc
            .perform(
                MockMvcRequestBuilders.get("/api/attachments/" + attachmentId)
                    .header("Authorization", "Bearer " + menteeToken))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", Matchers.containsString("attachment")))
            .andReturn();
    assertArrayEquals(payload, download.getResponse().getContentAsByteArray());

    // Non-participant (never enrolled) is refused list + download.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/posts")
                .header("Authorization", "Bearer " + outsiderToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONTENT_FORBIDDEN"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/attachments/" + attachmentId)
                .header("Authorization", "Bearer " + outsiderToken))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONTENT_FORBIDDEN"));

    // Owning mentor posts a notice; participant mentee reads it.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/notices")
                .header("Authorization", "Bearer " + mentorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"이번 주 공지입니다.\"}"))
        .andExpect(status().isCreated());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/meetings/" + meetingId + "/notices")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].body").value("이번 주 공지입니다."));

    // A non-owning mentor cannot author posts or notices on this meeting.
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/posts")
                .header("Authorization", "Bearer " + otherMentorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":1,\"body\":\"침입 시도\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONTENT_FORBIDDEN"));
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/notices")
                .header("Authorization", "Bearer " + otherMentorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"침입 공지\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONTENT_FORBIDDEN"));
  }

  @Test
  void uploadDisallowedType_returns400() throws Exception {
    String mentorToken = signupAndLogin("mentorX", "x-mentor", "MENTOR");
    String adminToken = createAdminAndLogin("adminX", "x-admin");
    long meetingId = createMeeting(mentorToken, 5);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());

    MvcResult postResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/posts")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"week\":1,\"body\":\"주차 자료\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    long postId =
        objectMapper.readTree(postResult.getResponse().getContentAsString()).get("id").asLong();

    // SVG is deliberately excluded from the whitelist (stored-XSS defence, TD-U6-2).
    MockMultipartFile svg =
        new MockMultipartFile(
            "file", "evil.svg", "image/svg+xml", "<svg/>".getBytes(StandardCharsets.UTF_8));
    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/api/posts/" + postId + "/attachments")
                .file(svg)
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ATTACHMENT_TYPE_NOT_ALLOWED"));
  }

  @Test
  void postWeekBeyondMeetingRange_returns400() throws Exception {
    String mentorToken = signupAndLogin("mentorW", "w-mentor", "MENTOR");
    long meetingId = createMeeting(mentorToken, 5); // weeks = 8

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/posts")
                .header("Authorization", "Bearer " + mentorToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"week\":99,\"body\":\"범위를 벗어난 주차\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CONTENT_VALIDATION"));
  }

  private long createMeeting(String mentorToken, int capacity) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        String.format(
                            "{\"title\":\"자료 테스트\",\"topic\":\"backend\",\"weeks\":8,"
                                + "\"capacity\":%d,\"format\":\"online\"}",
                            capacity)))
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
                        String.format("{\"nickname\":\"%s\",\"password\":\"password1\"}", nickname)))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
    return node.get("token").asText();
  }
}
