package com.pw.docvault.repository;

import com.pw.docvault.entity.security.ActivationToken;
import com.pw.docvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByToken(String token);
    void deleteByUser(User user);
}
