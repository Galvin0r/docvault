package com.pw.docvault.service.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.exception.RefreshTokenException;
import com.pw.docvault.repository.security.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value(value = "${app.security.jwtRefreshTokenExpiration}")
    private int jwtRefreshTokenExpiration;

    @Value(value = "${app.security.jwtRefreshTokenExpirationShort}")
    private int jwtRefreshTokenExpirationShort;

    @Transactional
    public RefreshToken getRefreshToken(User user, String deviceInfo, boolean rememberMe) {
        return refreshTokenRepository.findByUserIdAndDeviceInfo(user.getId(), deviceInfo).orElseGet(() -> {
            var refreshToken = new RefreshToken();
            refreshToken.setUser(user);
            refreshToken.setExpiresAt(Instant.now().plusSeconds(rememberMe ? jwtRefreshTokenExpiration : jwtRefreshTokenExpirationShort));
            refreshToken.setToken(UUID.randomUUID().toString());
            refreshToken.setDeviceInfo(deviceInfo);
            return refreshTokenRepository.save(refreshToken);
        });
    }

    @Transactional
    public RefreshToken rotateToken(Long tokenId) {
        var token = getRefreshTokenOrThrow(tokenId);
        token.setToken(UUID.randomUUID().toString());
        return token;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiresAt().isBefore(Instant.now())) {
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

    public RefreshToken getRefreshTokenOrThrow(String token) {
        return refreshTokenRepository.findByToken(token)
                                     .map(this::verifyExpiration)
                                     .orElseThrow(() -> new RefreshTokenException(ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED,
                                                                                  "Refresh token expired"));
    }

    @Transactional(readOnly = true)
    public RefreshToken getRefreshTokenOrThrow(Long tokenId) {
        return refreshTokenRepository.findById(tokenId).orElseThrow(
                () -> new NotFoundException(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }
}