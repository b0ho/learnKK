package com.learnkk.contract;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.learnkk.auth.dto.ProfileResponse;
import com.learnkk.auth.dto.SessionResponse;
import com.learnkk.auth.dto.UserResponse;
import com.learnkk.kernel.domain.MeetingStatus;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorPayload;
import com.learnkk.kernel.web.PageResponse;
import com.learnkk.meeting.dto.MeetingResponse;
import com.learnkk.meeting.dto.MeetingSummary;
import com.learnkk.meeting.dto.SurveyQuestionDto;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Contract-conformance layer: asserts that the JSON produced by the response DTOs matches the
 * shapes declared in {@code /contracts/openapi.yaml} (required fields present, no undeclared
 * fields). This ties the code's wire format to the published contract (#1).
 */
class OpenApiContractTest {

  private static OpenAPI openApi;
  private static Map<String, Schema> schemas;
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .setPropertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void loadContract() {
    Path contract = locateContract();
    assertThat(Files.exists(contract)).as("openapi.yaml must exist at %s", contract).isTrue();
    String content;
    try {
      content = Files.readString(contract);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Unable to read OpenAPI contract at " + contract, e);
    }
    SwaggerParseResult result = new OpenAPIV3Parser().readContents(content, null, null);
    openApi = result.getOpenAPI();
    assertThat(openApi)
        .as("OpenAPI document must parse (errors: %s)", result.getMessages())
        .isNotNull();
    schemas = openApi.getComponents().getSchemas();
    assertThat(schemas).isNotEmpty();
  }

  private static Path locateContract() {
    Path fromModule =
        Paths.get(System.getProperty("user.dir")).resolve("../contracts/openapi.yaml").normalize();
    if (Files.exists(fromModule)) {
      return fromModule;
    }
    return Paths.get("contracts/openapi.yaml").toAbsolutePath().normalize();
  }

  @Test
  void userResponse_conformsToSchema() throws Exception {
    assertConforms("UserResponse", new UserResponse(1L, "dev", "E-1", Role.MENTEE));
  }

  @Test
  void sessionResponse_conformsToSchema() throws Exception {
    assertConforms("SessionResponse", new SessionResponse("tok", Role.MENTOR));
  }

  @Test
  void profileResponse_conformsToSchema() throws Exception {
    assertConforms("ProfileResponse", new ProfileResponse("dev", "E-1", List.of("java"), "hi"));
  }

  @Test
  void meetingResponse_conformsToSchema() throws Exception {
    assertConforms(
        "MeetingResponse",
        new MeetingResponse(
            10L,
            1L,
            "t",
            "topic",
            8,
            null,
            null,
            5,
            "online",
            "c",
            MeetingStatus.RECRUITING,
            null));
  }

  @Test
  void meetingSummary_conformsToSchema() throws Exception {
    assertConforms(
        "MeetingSummary", new MeetingSummary(10L, "t", "topic", 8, 5, MeetingStatus.RECRUITING));
  }

  @Test
  void surveyQuestion_conformsToSchema() throws Exception {
    assertConforms(
        "SurveyQuestionDto", new SurveyQuestionDto(1L, 1, "질문", "TEXT", List.of("a", "b"), true));
  }

  @Test
  void errorPayload_conformsToSchema() throws Exception {
    assertConforms("ErrorPayload", ErrorPayload.of("SOME_CODE", "메시지"));
  }

  @Test
  void enrollmentResponse_conformsToSchema() throws Exception {
    assertConforms(
        "EnrollmentResponse",
        new com.learnkk.enrollment.dto.EnrollmentResponse(
            1L,
            10L,
            2L,
            com.learnkk.enrollment.domain.EnrollmentStatus.APPLIED,
            java.time.OffsetDateTime.parse("2026-01-01T00:00Z")));
  }

  @Test
  void applicantResponse_conformsToSchema() throws Exception {
    assertConforms(
        "ApplicantResponse",
        new com.learnkk.enrollment.dto.ApplicantResponse(
            2L, "멘티", java.time.OffsetDateTime.parse("2026-01-01T00:00Z")));
  }

  @Test
  void confirmRecruitmentRequest_conformsToSchema() throws Exception {
    assertConforms(
        "ConfirmRecruitmentRequest",
        new com.learnkk.meeting.dto.ConfirmRecruitmentRequest(false, "정원 미달"));
  }

  @Test
  void surveyAnswerRequest_conformsToSchema() throws Exception {
    assertConforms(
        "SurveyAnswerRequest",
        new com.learnkk.survey.dto.SurveyAnswerRequest(
            List.of(new com.learnkk.survey.dto.SurveyAnswerRequest.AnswerItem(100L, "답변"))));
  }

  @Test
  void surveyAnswerResponse_conformsToSchema() throws Exception {
    assertConforms(
        "SurveyAnswerResponse", new com.learnkk.survey.dto.SurveyAnswerResponse(100L, "답변"));
  }

  @Test
  void feedbackRequest_conformsToSchema() throws Exception {
    assertConforms("FeedbackRequest", new com.learnkk.survey.dto.FeedbackRequest("좋았습니다"));
  }

  @Test
  void feedbackResponse_conformsToSchema() throws Exception {
    assertConforms(
        "FeedbackResponse",
        new com.learnkk.survey.dto.FeedbackResponse(
            1L, 2L, "좋았습니다", java.time.OffsetDateTime.parse("2026-01-01T00:00Z")));
  }

  @Test
  void pageMeetingSummary_conformsToSchema() throws Exception {
    PageResponse<MeetingSummary> page =
        new PageResponse<>(
            List.of(new MeetingSummary(10L, "t", "topic", 8, 5, MeetingStatus.RECRUITING)),
            0,
            20,
            1,
            1);
    assertConforms("PageMeetingSummary", page);
  }

  @SuppressWarnings("unchecked")
  private void assertConforms(String schemaName, Object dto) throws Exception {
    Schema<?> schema = schemas.get(schemaName);
    assertThat(schema).as("schema %s must be defined", schemaName).isNotNull();

    JsonNode node = objectMapper.readTree(objectMapper.writeValueAsString(dto));
    Map<String, Schema> properties = schema.getProperties();
    assertThat(properties).as("schema %s must declare properties", schemaName).isNotEmpty();

    List<String> required = schema.getRequired();
    if (required != null) {
      for (String req : required) {
        assertThat(node.has(req))
            .as("schema %s requires field '%s' in the response body", schemaName, req)
            .isTrue();
      }
    }

    node.fieldNames()
        .forEachRemaining(
            field ->
                assertThat(properties.containsKey(field))
                    .as("field '%s' is not declared in schema %s", field, schemaName)
                    .isTrue());
  }
}
