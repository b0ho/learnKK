package com.learnkk.auth.service;

import com.learnkk.auth.dto.ProfileResponse;
import com.learnkk.auth.dto.ProfileUpdateRequest;
import com.learnkk.auth.entity.Profile;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.ProfileRepository;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.NotFoundException;
import com.learnkk.kernel.error.ValidationException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Profile read/update. Callers may only touch their own profile. */
@Service
public class UserService {

  static final int MAX_TAGS = 10;
  static final int MAX_INTRO_LENGTH = 500;

  private final UserRepository userRepository;
  private final ProfileRepository profileRepository;

  public UserService(UserRepository userRepository, ProfileRepository profileRepository) {
    this.userRepository = userRepository;
    this.profileRepository = profileRepository;
  }

  @Transactional(readOnly = true)
  public ProfileResponse getProfile(Long userId) {
    User user = loadUser(userId);
    Profile profile = profileRepository.findById(userId).orElseGet(() -> new Profile(userId));
    return toResponse(user, profile);
  }

  @Transactional
  public ProfileResponse updateProfile(
      Long requesterId, Long targetUserId, ProfileUpdateRequest request) {
    if (!requesterId.equals(targetUserId)) {
      throw new ForbiddenException(ErrorCodes.PROFILE_FORBIDDEN, "본인의 프로필만 수정할 수 있습니다.");
    }

    List<String> tags =
        request.tags() == null ? new ArrayList<>() : new ArrayList<>(request.tags());
    if (tags.size() > MAX_TAGS) {
      throw new ValidationException(
          ErrorCodes.PROFILE_TAGS_LIMIT, "관심 태그는 최대 " + MAX_TAGS + "개까지 등록할 수 있습니다.");
    }
    String intro = request.intro();
    if (intro != null && intro.length() > MAX_INTRO_LENGTH) {
      throw new ValidationException(ErrorCodes.PROFILE_INTRO_LIMIT, "소개는 500자 이하여야 합니다.");
    }

    User user = loadUser(targetUserId);
    Profile profile =
        profileRepository.findById(targetUserId).orElseGet(() -> new Profile(targetUserId));
    profile.setInterestTags(tags);
    profile.setIntro(intro);
    profileRepository.save(profile);
    return toResponse(user, profile);
  }

  private User loadUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new NotFoundException(ErrorCodes.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));
  }

  private ProfileResponse toResponse(User user, Profile profile) {
    return new ProfileResponse(
        user.getNickname(),
        user.getEmployeeNo(),
        profile.getInterestTags() == null ? new ArrayList<>() : profile.getInterestTags(),
        profile.getIntro());
  }
}
