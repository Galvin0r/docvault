package com.pw.docvault.repository.security;

import com.pw.docvault.entity.security.ActivationToken;
import com.pw.docvault.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {
    Optional<ActivationToken> findByToken(String token);
    @Modifying
    @Query("DELETE FROM ActivationToken a WHERE a.user = :user")
    void deleteByUser(@Param("user") User user);
}