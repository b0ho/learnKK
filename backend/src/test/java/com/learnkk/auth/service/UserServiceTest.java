package com.learnkk.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.learnkk.auth.dto.ProfileResponse;
import com.learnkk.auth.dto.ProfileUpdateRequest;
import com.learnkk.auth.entity.Profile;
import com.learnkk.auth.entity.User;
import com.learnkk.auth.repository.ProfileRepository;
import com.learnkk.auth.repository.UserRepository;
import com.learnkk.kernel.domain.Role;
import com.learnkk.kernel.error.ErrorCodes;
import com.learnkk.kernel.error.ForbiddenException;
import com.learnkk.kernel.error.ValidationException;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ProfileRepository profileRepository;

  @InjectMocks private UserService userService;

  @Test
  void getProfile_happyPath_returnsProfile() {
    User user = new User("dev", "h", "E-1", Role.MENTOR);
    Profile profile = new Profile(1L);
    profile.setInterestTags(List.of("java"));
    profile.setIntro("hello");
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(profileRepository.findById(1L)).thenReturn(Optional.of(profile));

    ProfileResponse response = userService.getProfile(1L);

    assertThat(response.nickname()).isEqualTo("dev");
    assertThat(response.tags()).containsExactly("java");
    assertThat(response.intro()).isEqualTo("hello");
  }

  @Test
  void updateProfile_happyPath_persists() {
    User user = new User("dev", "h", "E-1", Role.MENTOR);
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(profileRepository.findById(1L)).thenReturn(Optional.of(new Profile(1L)));

    ProfileResponse response =
        userService.updateProfile(1L, 1L, new ProfileUpdateRequest(List.of("a", "b"), "intro"));

    assertThat(response.tags()).containsExactly("a", "b");
    assertThat(response.intro()).isEqualTo("intro");
    verify(profileRepository).save(any(Profile.class));
  }

  @Test
  void updateProfile_notOwner_forbidden403() {
    assertThatThrownBy(
            () -> userService.updateProfile(1L, 2L, new ProfileUpdateRequest(List.of(), null)))
        .isInstanceOf(ForbiddenException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.PROFILE_FORBIDDEN);
    verify(profileRepository, never()).save(any());
  }

  @Test
  void updateProfile_tooManyTags_validation400() {
    List<String> tags = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toList();

    assertThatThrownBy(
            () -> userService.updateProfile(1L, 1L, new ProfileUpdateRequest(tags, null)))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.PROFILE_TAGS_LIMIT);
  }

  @Test
  void updateProfile_introTooLong_validation400() {
    String longIntro = "x".repeat(501);

    assertThatThrownBy(
            () -> userService.updateProfile(1L, 1L, new ProfileUpdateRequest(List.of(), longIntro)))
        .isInstanceOf(ValidationException.class)
        .extracting("code")
        .isEqualTo(ErrorCodes.PROFILE_INTRO_LIMIT);
  }
}
