package com.learnkk.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** User profile (1:1 with {@link User}); the user id is the shared primary key. */
@Entity
@Table(name = "profiles")
public class Profile {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "interest_tags", columnDefinition = "text[]")
  private List<String> interestTags = new ArrayList<>();

  @Column(length = 500)
  private String intro;

  protected Profile() {}

  public Profile(Long userId) {
    this.userId = userId;
  }

  public Long getUserId() {
    return userId;
  }

  public List<String> getInterestTags() {
    return interestTags;
  }

  public void setInterestTags(List<String> interestTags) {
    this.interestTags = interestTags;
  }

  public String getIntro() {
    return intro;
  }

  public void setIntro(String intro) {
    this.intro = intro;
  }
}
