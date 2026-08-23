package com.learnkk.integration;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.kernel.domain.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * End-to-end messaging: the mentor↔enrolled-mentee boundary is enforced through the full stack — an
 * enrolled pair may message (both directions land in one thread), reading clears the unread count
 * idempotently, an unrelated pair is 403, and an admin may message anyone.
 */
class MessageIntegrationTest extends AbstractIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @Test
  void messagingBoundary_endToEnd() throws Exception {
    String mentorToken = signupAndLogin("msgMentor", "msg-mentor", "MENTOR");
    String adminToken = createAdminAndLogin("msgAdmin", "msg-admin");
    String menteeToken = signupAndLogin("msgMentee", "msg-mentee", "MENTEE");
    String strangerToken = signupAndLogin("msgStranger", "msg-stranger", "MENTEE");

    long mentorId = userId("msgMentor");
    long menteeId = userId("msgMentee");

    // Mentor opens a meeting; admin approves; mentee enrolls -> the pair is now related.
    long meetingId = createMeeting(mentorToken);
    approve(meetingId, adminToken);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/meetings/" + meetingId + "/enrollments")
                .header("Authorization", "Bearer " + menteeToken))
        .andExpect(status().isCreated());

    // Mentee -> mentor: allowed (mentee applied to the mentor's meeting).
    long threadId =
        objectMapper
            .readTree(send(menteeToken, mentorId, "멘토님 질문 있어요").getResponse().getContentAsString())
            .get("threadId")
            .asLong();

    // Mentor sees one thread with one unread message.
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/messages/threads")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].partnerNickname").value("msgMentee"))
        .andExpect(jsonPath("$[0].unreadCount").value(1));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/messages/unread-count")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(1));

    // Reading the thread clears the unread count (idempotent).
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/messages/threads/" + threadId)
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/messages/unread-count")
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(0));

    // Mentor -> mentee reply lands in the SAME thread (normalized pair uniqueness).
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/api/messages/threads/" + threadId)
                .header("Authorization", "Bearer " + mentorToken))
        .andExpect(status().isOk());
    long replyThreadId =
        objectMapper
            .readTree(send(mentorToken, menteeId, "네 말씀하세요").getResponse().getContentAsString())
            .get("threadId")
            .asLong();
    org.junit.jupiter.api.Assertions.assertEquals(threadId, replyThreadId);

    // Stranger (never enrolled) -> mentor: forbidden.
    mockMvc
        .perform(sendRequest(strangerToken, mentorId, "안녕하세요"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("MESSAGING_FORBIDDEN"));

    // Admin -> anyone: allowed.
    mockMvc.perform(sendRequest(adminToken, menteeId, "공지입니다")).andExpect(status().isCreated());

    // Self-send: rejected.
    mockMvc
        .perform(sendRequest(menteeToken, menteeId, "메모"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MESSAGING_SELF"));
  }

  private long userId(String nickname) {
    return userRepository.findByNickname(nickname).orElseThrow().getId();
  }

  private MvcResult send(String token, long recipientId, String body) throws Exception {
    return mockMvc
        .perform(sendRequest(token, recipientId, body))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private org.springframework.test.web.servlet.RequestBuilder sendRequest(
      String token, long recipientId, String body) {
    return MockMvcRequestBuilders.post("/api/messages")
        .header("Authorization", "Bearer " + token)
        .contentType(MediaType.APPLICATION_JSON)
        .content(String.format("{\"recipientId\":%d,\"body\":\"%s\"}", recipientId, body));
  }

  private void approve(long meetingId, String adminToken) throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/api/admin/meetings/" + meetingId + "/approve")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk());
  }

  private long createMeeting(String mentorToken) throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/api/meetings")
                    .header("Authorization", "Bearer " + mentorToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"title\":\"쪽지 테스트\",\"topic\":\"backend\",\"weeks\":8,"
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
