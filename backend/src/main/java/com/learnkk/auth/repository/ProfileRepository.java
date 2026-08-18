package com.learnkk.auth.repository;

import com.learnkk.auth.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfileRepository extends JpaRepository<Profile, Long> {}
