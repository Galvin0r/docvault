package com.pw.docvault.repository.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    @Modifying
    @Query("DELETE FROM PasswordResetToken a WHERE a.user = :user")
    void deleteByUser(User user);
}