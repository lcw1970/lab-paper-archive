package com.lab.paperarchive.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // DB에 LOWER(email) UNIQUE 인덱스가 있으므로 대소문자 무시로 조회한다
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<User> findByStatusOrderByCreatedAtAsc(Status status);

    List<User> findAllByOrderByStatusAscCreatedAtDesc();

    long countByStatus(Status status);
}
