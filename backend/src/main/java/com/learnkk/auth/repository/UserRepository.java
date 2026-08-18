package com.learnkk.auth.repository;

import com.learnkk.auth.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByNickname(String nickname);

  boolean existsByNickname(String nickname);

  boolean existsByEmployeeNo(String employeeNo);
}
