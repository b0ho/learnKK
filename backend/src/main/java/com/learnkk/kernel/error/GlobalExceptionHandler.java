package com.learnkk.kernel.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Translates domain exceptions and Bean Validation failures into the uniform {@link ErrorPayload}
 * body with the correct HTTP status (400/401/403/404/409).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorPayload> handleDomain(DomainException ex) {
    return ResponseEntity.status(ex.getHttpStatus()).body(ex.toPayload());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorPayload> handleBeanValidation(MethodArgumentNotValidException ex) {
    Map<String, Object> details = new LinkedHashMap<>();
    for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
      details.putIfAbsent(fe.getField(), fe.getDefaultMessage());
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorPayload.of(ErrorCodes.VALIDATION_FAILED, "입력값이 올바르지 않습니다.", details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorPayload> handleConstraintViolation(ConstraintViolationException ex) {
    Map<String, Object> details = new LinkedHashMap<>();
    for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
      details.putIfAbsent(v.getPropertyPath().toString(), v.getMessage());
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorPayload.of(ErrorCodes.VALIDATION_FAILED, "입력값이 올바르지 않습니다.", details));
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ErrorPayload> handleBadRequest(Exception ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorPayload.of(ErrorCodes.VALIDATION_FAILED, "요청 파라미터가 올바르지 않습니다."));
  }
}
