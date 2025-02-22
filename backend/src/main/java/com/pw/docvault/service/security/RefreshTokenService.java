package com.pw.docvault.service.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.TokenRefreshException;
import com.pw.docvault.repository.RefreshTokenRepository;
import com.pw.docvault.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value(value = "${app.security.jwtRefreshTokenExpirationMs}")

    private int jwtRefreshTokenExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken getRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(jwtRefreshTokenExpiration));
        refreshToken.setToken(UUID.randomUUID().toString());

        refreshTokenRepository.deleteByUserId(user.getId());
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token was expired. Please signin again.");
        }
        return token;
    }

    @Transactional
    public int deleteByUserId(Long userId) {
        return refreshTokenRepository.deleteByUserId(userId);
    }
}
