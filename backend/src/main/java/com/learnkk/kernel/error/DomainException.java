package com.learnkk.kernel.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** Base class for domain exceptions carrying an error code and HTTP status mapping. */
public abstract class DomainException extends RuntimeException {

  private final String code;
  private final HttpStatus httpStatus;
  private final transient Map<String, Object> details;

  protected DomainException(String code, HttpStatus httpStatus, String message) {
    this(code, httpStatus, message, null);
  }

  protected DomainException(
      String code, HttpStatus httpStatus, String message, Map<String, Object> details) {
    super(message);
    this.code = code;
    this.httpStatus = httpStatus;
    this.details = details;
  }

  public String getCode() {
    return code;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public ErrorPayload toPayload() {
    return ErrorPayload.of(code, getMessage(), details);
  }
}
