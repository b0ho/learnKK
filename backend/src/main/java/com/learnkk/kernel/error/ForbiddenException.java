package com.learnkk.kernel.error;

import org.springframework.http.HttpStatus;

/** 403 Forbidden — authenticated but not authorized for this action. */
public class ForbiddenException extends DomainException {

  public ForbiddenException(String code, String message) {
    super(code, HttpStatus.FORBIDDEN, message);
  }
}
