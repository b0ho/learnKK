package com.learnkk.kernel.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ValidationException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageRequestFactoryTest {

  private static final Set<String> ALLOWED = Set.of("id", "createdAt");

  @Test
  void defaults_whenParamsNull() {
    Pageable pageable = PageRequestFactory.of(null, null, null, ALLOWED);

    assertThat(pageable.getPageNumber()).isZero();
    assertThat(pageable.getPageSize()).isEqualTo(PageRequestFactory.DEFAULT_SIZE);
    assertThat(pageable.getSort().isSorted()).isFalse();
  }

  @Test
  void clampsSizeToMax() {
    Pageable pageable = PageRequestFactory.of(2, 500, null, ALLOWED);

    assertThat(pageable.getPageSize()).isEqualTo(PageRequestFactory.MAX_SIZE);
    assertThat(pageable.getPageNumber()).isEqualTo(2);
  }

  @Test
  void clampsSizeToMinimumOne() {
    Pageable pageable = PageRequestFactory.of(-3, 0, null, ALLOWED);

    assertThat(pageable.getPageSize()).isEqualTo(1);
    assertThat(pageable.getPageNumber()).isZero(); // negative page clamped to 0
  }

  @Test
  void parsesSortDescending() {
    Pageable pageable = PageRequestFactory.of(0, 20, "createdAt,desc", ALLOWED);

    Sort.Order order = pageable.getSort().getOrderFor("createdAt");
    assertThat(order).isNotNull();
    assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  void parsesSortAscendingByDefault() {
    Pageable pageable = PageRequestFactory.of(0, 20, "id", ALLOWED);

    Sort.Order order = pageable.getSort().getOrderFor("id");
    assertThat(order).isNotNull();
    assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  void unknownSortField_throwsValidation400() {
    assertThatThrownBy(() -> PageRequestFactory.of(0, 20, "password", ALLOWED))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.INVALID_SORT_FIELD);
  }
}
