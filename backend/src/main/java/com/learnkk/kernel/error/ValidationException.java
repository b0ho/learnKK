package com.learnkk.kernel.error;

import java.util.Map;
import org.springframework.http.HttpStatus;

/** 400 Bad Request — input validation or business-rule precondition failure. */
public class ValidationException extends DomainException {

  public ValidationException(String code, String message) {
    super(code, HttpStatus.BAD_REQUEST, message);
  }

  public ValidationException(String code, String message, Map<String, Object> details) {
    super(code, HttpStatus.BAD_REQUEST, message, details);
  }
}
