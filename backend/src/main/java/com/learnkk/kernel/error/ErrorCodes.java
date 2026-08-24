package com.learnkk.kernel.error;

/**
 * Canonical error code constants. Format: UPPER_SNAKE {@code <DOMAIN>_<REASON>}. Messages are
 * Korean and defined where the exception is thrown.
 */
public final class ErrorCodes {

  private ErrorCodes() {}

  // --- Common / validation ---
  public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
  public static final String INVALID_SORT_FIELD = "INVALID_SORT_FIELD";
  public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

  // --- Auth domain ---
  public static final String AUTH_UNAUTHENTICATED = "AUTH_UNAUTHENTICATED";
  public static final String AUTH_INVALID_CREDENTIALS = "AUTH_INVALID_CREDENTIALS";
  public static final String AUTH_SESSION_EXPIRED = "AUTH_SESSION_EXPIRED";
  public static final String ADMIN_SIGNUP_FORBIDDEN = "ADMIN_SIGNUP_FORBIDDEN";
  public static final String DUPLICATE_EMPLOYEE_NO = "DUPLICATE_EMPLOYEE_NO";
  public static final String DUPLICATE_NICKNAME = "DUPLICATE_NICKNAME";

  // --- User / profile domain ---
  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
  public static final String PROFILE_FORBIDDEN = "PROFILE_FORBIDDEN";
  public static final String PROFILE_TAGS_LIMIT = "PROFILE_TAGS_LIMIT";
  public static final String PROFILE_INTRO_LIMIT = "PROFILE_INTRO_LIMIT";

  // --- Meeting domain ---
  public static final String MEETING_NOT_FOUND = "MEETING_NOT_FOUND";
  public static final String MEETING_FORBIDDEN = "MEETING_FORBIDDEN";
  public static final String MEETING_VALIDATION = "MEETING_VALIDATION";
  public static final String MEETING_INVALID_TRANSITION = "MEETING_INVALID_TRANSITION";
  public static final String MEETING_QUESTIONS_LOCKED = "MEETING_QUESTIONS_LOCKED";
  public static final String MEETING_SESSIONS_NOT_ENDED = "MEETING_SESSIONS_NOT_ENDED";

  // --- Enrollment domain ---
  public static final String ENROLLMENT_NOT_FOUND = "ENROLLMENT_NOT_FOUND";
  public static final String ENROLLMENT_FORBIDDEN = "ENROLLMENT_FORBIDDEN";
  public static final String ENROLLMENT_FULL = "ENROLLMENT_FULL";
  public static final String ENROLLMENT_DUPLICATE = "ENROLLMENT_DUPLICATE";
  public static final String ENROLLMENT_NOT_OPEN = "ENROLLMENT_NOT_OPEN";
  public static final String ENROLLMENT_CANCEL_FORBIDDEN = "ENROLLMENT_CANCEL_FORBIDDEN";

  // --- Content domain (U6: posts / attachments / notices) ---
  public static final String POST_NOT_FOUND = "POST_NOT_FOUND";
  public static final String CONTENT_FORBIDDEN = "CONTENT_FORBIDDEN";
  public static final String CONTENT_VALIDATION = "CONTENT_VALIDATION";
  public static final String ATTACHMENT_NOT_FOUND = "ATTACHMENT_NOT_FOUND";
  public static final String ATTACHMENT_TYPE_NOT_ALLOWED = "ATTACHMENT_TYPE_NOT_ALLOWED";
  public static final String ATTACHMENT_TOO_LARGE = "ATTACHMENT_TOO_LARGE";
  public static final String ATTACHMENT_LIMIT = "ATTACHMENT_LIMIT";

  // --- Messaging domain ---
  public static final String MESSAGING_FORBIDDEN = "MESSAGING_FORBIDDEN";
  public static final String MESSAGING_SELF = "MESSAGING_SELF";
  public static final String MESSAGING_EMPTY_BODY = "MESSAGING_EMPTY_BODY";
  public static final String MESSAGING_RECIPIENT_NOT_FOUND = "MESSAGING_RECIPIENT_NOT_FOUND";
  public static final String MESSAGING_THREAD_NOT_FOUND = "MESSAGING_THREAD_NOT_FOUND";

  // --- Session domain (U5) ---
  public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";
  public static final String SESSION_FORBIDDEN = "SESSION_FORBIDDEN";
  public static final String SESSION_MEETING_NOT_ACTIVE = "SESSION_MEETING_NOT_ACTIVE";
  public static final String ATTENDANCE_WINDOW_CLOSED = "ATTENDANCE_WINDOW_CLOSED";
  public static final String ATTENDANCE_NOT_PARTICIPANT = "ATTENDANCE_NOT_PARTICIPANT";
  public static final String COMPLETION_NOT_FOUND = "COMPLETION_NOT_FOUND";
  public static final String COMPLETION_FORBIDDEN = "COMPLETION_FORBIDDEN";
  public static final String COMPLETION_NOT_ELIGIBLE = "COMPLETION_NOT_ELIGIBLE";
  public static final String COMPLETION_ALREADY_APPROVED = "COMPLETION_ALREADY_APPROVED";

  // --- Survey / feedback domain (U8) ---
  public static final String PRESURVEY_NOT_OPEN = "PRESURVEY_NOT_OPEN";
  public static final String PRESURVEY_REQUIRED_MISSING = "PRESURVEY_REQUIRED_MISSING";
  public static final String PRESURVEY_FORBIDDEN = "PRESURVEY_FORBIDDEN";
  public static final String PRESURVEY_NOT_FOUND = "PRESURVEY_NOT_FOUND";
  public static final String FEEDBACK_FORBIDDEN = "FEEDBACK_FORBIDDEN";
  public static final String FEEDBACK_NOT_OPEN = "FEEDBACK_NOT_OPEN";
}
