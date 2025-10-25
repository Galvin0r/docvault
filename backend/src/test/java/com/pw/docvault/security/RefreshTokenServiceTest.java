package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.exception.RefreshTokenException;
import com.pw.docvault.repository.security.RefreshTokenRepository;
import com.pw.docvault.service.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(10L);

        ReflectionTestUtils.setField(refreshTokenService, "jwtRefreshTokenExpiration", 3600);
        ReflectionTestUtils.setField(refreshTokenService, "jwtRefreshTokenExpirationShort", 600);
    }

    // get refresh token

    @Test
    void getRefreshTokenReturnsExistingWhenFoundForDevice() {
        String deviceInfo = "45a-53h45";
        RefreshToken existing = new RefreshToken();
        existing.setId(1L);
        existing.setToken("existing");
        existing.setUser(user);
        existing.setDeviceInfo(deviceInfo);
        existing.setExpiresAt(Instant.now().plusSeconds(1000));

        when(refreshTokenRepository.findByUserIdAndDeviceInfo(10L, deviceInfo))
                .thenReturn(Optional.of(existing));

        RefreshToken result = refreshTokenService.getRefreshToken(user, deviceInfo, false);

        assertThat(result).isSameAs(existing);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void getRefreshTokenCreatesNewWhenNoneForDeviceAndUsesShortExpirationIfRememberMeFalse() {
        String deviceInfo = "45a-53h45";
        when(refreshTokenRepository.findByUserIdAndDeviceInfo(10L, deviceInfo))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RefreshToken created = refreshTokenService.getRefreshToken(user, deviceInfo, false);

        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getDeviceInfo()).isEqualTo(deviceInfo);
        assertThat(created.getToken()).isNotBlank();

        assertThat(created.getExpiresAt())
                .isAfter(Instant.now().plusSeconds(590))
                .isBefore(Instant.now().plusSeconds(610));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void getRefreshTokenCreatesNewWithLongExpirationWhenRememberMeTrue() {
        String deviceInfo = "45a-53h45";
        when(refreshTokenRepository.findByUserIdAndDeviceInfo(10L, deviceInfo))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RefreshToken created = refreshTokenService.getRefreshToken(user, deviceInfo, true);

        assertThat(created.getExpiresAt())
                .isAfter(Instant.now().plusSeconds(3500))
                .isBefore(Instant.now().plusSeconds(3700));
    }

    // rotate token

    @Test
    void rotateTokenReplacesTokenValueWhenFound() {
        var oldToken = "old-token";
        RefreshToken stored = new RefreshToken();
        stored.setId(99L);
        stored.setToken(oldToken);
        when(refreshTokenRepository.findById(99L)).thenReturn(Optional.of(stored));

        RefreshToken rotated = refreshTokenService.rotateToken(99L);

        assertThat(rotated).isSameAs(stored);
        assertThat(rotated.getToken()).isNotEqualTo(oldToken).isNotBlank();
    }

    @Test
    void rotateTokenThrowsNotFoundWhenMissing() {
        when(refreshTokenRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotateToken(123L))
                .isInstanceOf(NotFoundException.class);
    }

    // verify expiration

    @Test
    void verifyExpirationReturnsTokenWhenNotExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(Instant.now().plusSeconds(30));

        RefreshToken result = refreshTokenService.verifyExpiration(token);

        assertThat(result).isSameAs(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpirationDeletesAndThrowsWhenExpired() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> refreshTokenService.verifyExpiration(token))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(token);
    }

    // delete

    @Test
    void deleteByUserIdAndDeviceInfo_delegatesToRepository() {
        var deviceInfo = "45a-53h45";
        refreshTokenService.deleteByUserIdAndDeviceInfo(10L, deviceInfo);
        verify(refreshTokenRepository).deleteByUserIdAndDeviceInfo(10L, deviceInfo);
    }

    // ger token or throw

    @Test
    void getRefreshTokenOrThrowReturnsNonExpiredToken() {
        var tokenValue = "abc";
        RefreshToken token = new RefreshToken();
        token.setToken(tokenValue);
        token.setExpiresAt(Instant.now().plusSeconds(100));

        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(token));

        RefreshToken result = refreshTokenService.getRefreshTokenOrThrow(tokenValue);

        assertThat(result).isSameAs(token);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void getRefreshTokenOrThrow_throwsWhenMissing() {
        var token = "missing";
        when(refreshTokenRepository.findByToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.getRefreshTokenOrThrow(token))
                .isInstanceOf(RefreshTokenException.class);
    }

    @Test
    void getRefreshTokenOrThrow_throwsWhenExpired_andDeletes() {
        var tokenValue = "expired";
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(tokenValue);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(5));
        when(refreshTokenRepository.findByToken(tokenValue)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.getRefreshTokenOrThrow(tokenValue))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expiredToken);
    }
}
