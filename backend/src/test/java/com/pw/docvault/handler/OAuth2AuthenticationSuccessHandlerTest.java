package com.pw.docvault.handler;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import com.pw.docvault.util.Constants;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    private static final String FRONTEND_URL = "https://frontend.example/app";
    private static final String DEVICE_COOKIE_NAME = "deviceId";
    private static final String DEVICE_ID = "dev-123";
    private static final String EMAIL_ALICE = "alice@example.com";
    private static final String EMAIL_BOB = "bob@example.com";
    private static final String EMAIL_UNKNOWN = "nobody@example.com";
    private static final String JWT = "jwt-token";
    private static final String JWT_ALT = "jwt";
    private static final String REFRESH = "refresh-token";
    private static final String REFRESH_ALT = "rt";
    private static final String JWT_COOKIE_NAME = "JWT";
    private static final String REFRESH_COOKIE_NAME = "JWT_REFRESH";

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    @InjectMocks
    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", FRONTEND_URL);
    }

    private OAuth2AuthenticationToken googleAuth(String email) {
        OAuth2User principal = new OAuth2User() {
            @Override public Map<String, Object> getAttributes() { return Map.of(Constants.ATTRIBUTE_GOOGLE_EMAIL, email); }
            @Override public List<SimpleGrantedAuthority> getAuthorities() { return List.of(new SimpleGrantedAuthority("ROLE_USER")); }
            @Override public String getName() { return email; }
            @SuppressWarnings("unchecked")
            @Override public <A> A getAttribute(String name) { return (A) getAttributes().get(name); }
        };
        return new OAuth2AuthenticationToken(principal, principal.getAuthorities(), Constants.PROVIDER_GOOGLE);
    }

    @Test
    void onAuthenticationSuccess_setsCookies_redirects_and_usesDeviceId() throws IOException {
        var request = new MockHttpServletRequest();
        request.setCookies(new Cookie(DEVICE_COOKIE_NAME, DEVICE_ID));
        var response = new MockHttpServletResponse();
        var authenticationToken = googleAuth(EMAIL_ALICE);

        var user = new User();
        when(userRepository.findByEmail(EMAIL_ALICE)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(JWT);

        var refreshToken = new RefreshToken();
        refreshToken.setToken(REFRESH);
        when(refreshTokenService.getRefreshToken(user, DEVICE_ID, true)).thenReturn(refreshToken);

        var jwtCookie = ResponseCookie.from(JWT_COOKIE_NAME, JWT).path("/").httpOnly(true).build();
        var refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, REFRESH).path("/").httpOnly(true).build();
        when(jwtService.generateJwtCookie(JWT)).thenReturn(jwtCookie);
        when(jwtService.generateRefreshJwtCookie(REFRESH)).thenReturn(refreshCookie);

        handler.onAuthenticationSuccess(request, response, authenticationToken);

        var setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).contains(jwtCookie.toString(), refreshCookie.toString());
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL);
        verify(refreshTokenService).getRefreshToken(user, DEVICE_ID, true);
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.getOauth2Provider()).isEqualTo(Constants.PROVIDER_GOOGLE);
    }

    @Test
    void onAuthenticationSuccess_works_withoutDeviceCookie() throws IOException {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var authenticationToken = googleAuth(EMAIL_BOB);

        var user = new User();
        when(userRepository.findByEmail(EMAIL_BOB)).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn(JWT_ALT);

        var refreshToken = new RefreshToken();
        refreshToken.setToken(REFRESH_ALT);
        when(refreshTokenService.getRefreshToken(eq(user), isNull(), eq(true))).thenReturn(refreshToken);

        when(jwtService.generateJwtCookie(JWT_ALT)).thenReturn(ResponseCookie.from(JWT_COOKIE_NAME, JWT_ALT).build());
        when(jwtService.generateRefreshJwtCookie(REFRESH_ALT)).thenReturn(ResponseCookie.from(REFRESH_COOKIE_NAME, REFRESH_ALT).build());

        handler.onAuthenticationSuccess(request, response, authenticationToken);

        verify(refreshTokenService).getRefreshToken(eq(user), isNull(), eq(true));
        assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND_URL);
    }

    @Test
    void onAuthenticationSuccess_throws_whenUserNotFound() {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var authenticationToken = googleAuth(EMAIL_UNKNOWN);

        when(userRepository.findByEmail(EMAIL_UNKNOWN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(request, response, authenticationToken))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(jwtService, refreshTokenService);
    }
}
