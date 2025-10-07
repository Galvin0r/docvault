package com.pw.docvault.service.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.RefreshTokenException;
import com.pw.docvault.repository.security.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value(value = "${app.security.jwtRefreshTokenExpiration}")
    private int jwtRefreshTokenExpiration;

    @Value(value = "${app.security.jwtRefreshTokenExpirationShort}")
    private int jwtRefreshTokenExpirationShort;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public RefreshToken getRefreshToken(User user, String deviceInfo, boolean rememberMe) {
        return refreshTokenRepository.findByUserIdAndDeviceInfo(user.getId(), deviceInfo).orElseGet(() -> {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(rememberMe ? jwtRefreshTokenExpiration : jwtRefreshTokenExpirationShort));
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setDeviceInfo(deviceInfo);
            return refreshTokenRepository.save(refreshToken);
        });
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED,
                                            "Refresh token expired.");
        }
        return token;
    }

    @Transactional
    public void deleteByUserIdAndDeviceInfo(Long userId, String deviceInfo) {
        refreshTokenRepository.deleteByUserIdAndDeviceInfo(userId, deviceInfo);
    }
}
