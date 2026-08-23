package com.learnkk.kernel.web;

import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ValidationException;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

/**
 * Builds a {@link Pageable} from raw request parameters with defensive defaults: page defaults to
 * 0, size defaults to 20 and is clamped to [1, 100]. An unknown sort field yields 400
 * INVALID_SORT_FIELD.
 */
public final class PageRequestFactory {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  private PageRequestFactory() {}

  /**
   * @param page requested page (nullable, negatives clamped to 0)
   * @param size requested size (nullable, clamped to [1, 100])
   * @param sort optional sort expression {@code field} or {@code field,asc|desc}
   * @param allowedSortFields whitelist of sortable fields
   */
  public static Pageable of(
      Integer page, Integer size, String sort, Set<String> allowedSortFields) {
    int p = (page == null || page < 0) ? 0 : page;
    int s = (size == null) ? DEFAULT_SIZE : size;
    if (s < 1) {
      s = 1;
    } else if (s > MAX_SIZE) {
      s = MAX_SIZE;
    }

    if (!StringUtils.hasText(sort)) {
      return PageRequest.of(p, s);
    }

    String[] parts = sort.split(",", 2);
    String field = parts[0].trim();
    if (!allowedSortFields.contains(field)) {
      throw new ValidationException(
          ErrorCodes.INVALID_SORT_FIELD,
          "정렬할 수 없는 필드입니다: " + field,
          Map.of("allowed", allowedSortFields));
    }
    Sort.Direction direction = Sort.Direction.ASC;
    if (parts.length == 2 && "desc".equalsIgnoreCase(parts[1].trim())) {
      direction = Sort.Direction.DESC;
    }
    return PageRequest.of(p, s, Sort.by(direction, field));
  }
}
