package com.learnkk.auth.repository;

import com.learnkk.auth.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionRepository extends JpaRepository<Session, String> {}
