package com.pw.docvault.repository;

import com.pw.docvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByLogin(String login);
    Optional<User> findById(Long id);
    Optional<String> getEmailByLogin(String login);
}