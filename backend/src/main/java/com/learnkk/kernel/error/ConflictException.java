package com.learnkk.kernel.error;

import org.springframework.http.HttpStatus;

/** 409 Conflict — state-transition conflict or uniqueness violation. */
public class ConflictException extends DomainException {

  public ConflictException(String code, String message) {
    super(code, HttpStatus.CONFLICT, message);
  }
}
