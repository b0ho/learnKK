package com.learnkk.kernel.error;

import java.util.Map;

/**
 * Uniform error body returned for every non-2xx response.
 *
 * @param code UPPER_SNAKE domain error code ({@code <DOMAIN>_<REASON>})
 * @param message human-readable Korean message
 * @param details optional field-level or contextual details (may be null)
 */
public record ErrorPayload(String code, String message, Map<String, Object> details) {

  public static ErrorPayload of(String code, String message) {
    return new ErrorPayload(code, message, null);
  }

  public static ErrorPayload of(String code, String message, Map<String, Object> details) {
    return new ErrorPayload(code, message, details);
  }
}
