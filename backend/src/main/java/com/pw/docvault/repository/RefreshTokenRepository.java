package com.pw.docvault.repository;

import com.pw.docvault.entity.security.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserIdAndDeviceInfo(Long userId, String deviceInfo);
}
