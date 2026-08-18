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
}
