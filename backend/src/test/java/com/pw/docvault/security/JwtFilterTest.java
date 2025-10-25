package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.RefreshTokenException;
import com.pw.docvault.service.security.JwtFilter;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import com.pw.docvault.service.security.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
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
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class JwtFilterTest {

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtFilter jwtFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    private User user(String login, String email) {
        var user = new User();
        user.setLogin(login);
        user.setPassword("{noop}pass");
        user.setEmail(email);
        user.setRoles(List.of());
        return user;
    }

    @Test
    void bypassesAuthEndpoints() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/auth/login");
        var response = new MockHttpServletResponse();

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoMoreInteractions(refreshTokenService,  userDetailsService, jwtService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authenticatesWithValidJwt() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/groups");
        var response = new MockHttpServletResponse();
        var accessJwt = "valid-access";
        var user = user("valid-user", "valid-email");

        when(jwtService.getJwtFromCookies(request)).thenReturn(accessJwt);
        when(jwtService.getJwtRefreshFromCookies(request)).thenReturn(null);
        when(jwtService.extractUsername(accessJwt)).thenReturn(user.getUsername());
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtService.isTokenValid(accessJwt, user)).thenReturn(true);

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        var authentication =  SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(user.getUsername());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void  shouldNotAuthenticateWhenAccessJwtInvalidAndRefreshNull() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/groups");
        var response = new MockHttpServletResponse();
        var accessJwt = "invalid-access";
        var user = user("valid-user", "valid-email");

        when(jwtService.getJwtFromCookies(request)).thenReturn(accessJwt);
        when(jwtService.getJwtRefreshFromCookies(request)).thenReturn(null);
        when(jwtService.extractUsername(accessJwt)).thenReturn(user.getUsername());
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtService.isTokenValid(accessJwt, user)).thenReturn(false);

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void shouldReissueTokensWhenAccessExpiredAndRefreshValid() throws  Exception {
        var request = new MockHttpServletRequest("GET", "/api/groups");
        var response = new MockHttpServletResponse();
        var accessJwt = "expired-access";
        var refreshToken = "refresh-old";
        var user = user("valid-user", "valid-email");
        var newAccess = "new-access";
        var newRefresh = "new-refresh";

        when(jwtService.getJwtFromCookies(request)).thenReturn(accessJwt);
        when(jwtService.getJwtRefreshFromCookies(request)).thenReturn(refreshToken);
        when(jwtService.extractUsername(accessJwt)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        RefreshToken storedRefreshToken = new RefreshToken();
        storedRefreshToken.setId(100L);
        storedRefreshToken.setUser(user);
        storedRefreshToken.setToken(refreshToken);

        when(refreshTokenService.getRefreshTokenOrThrow(refreshToken)).thenReturn(storedRefreshToken);
        when(userDetailsService.loadUserByUsername(user.getUsername())).thenReturn(user);
        when(jwtService.generateToken(user)).thenReturn(newAccess);

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setId(101L);
        newRefreshToken.setUser(user);
        newRefreshToken.setToken(newRefresh);

        when(refreshTokenService.rotateToken(storedRefreshToken.getId())).thenReturn(newRefreshToken);

        ResponseCookie accessCookie = ResponseCookie.from("jwt", newAccess).path("/api").httpOnly(true).build();
        ResponseCookie refreshCookie = ResponseCookie.from("jwt_refresh", newRefresh).path("/api").httpOnly(true).build();

        when(jwtService.generateJwtCookie(newAccess)).thenReturn(accessCookie);
        when(jwtService.generateRefreshJwtCookie(newRefresh)).thenReturn(refreshCookie);

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        List<String> setCookieHeaders = response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).hasSize(2);
        assertThat(String.join(";", setCookieHeaders))
                .contains("jwt=new-access")
                .contains("jwt_refresh=new-refresh");

        var authentication =  SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo(user.getUsername());
    }

    @Test
    void shouldPassThroughWhenAccessExpiredAndNoRefreshProvided() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/groups");
        var response = new MockHttpServletResponse();
        var accessJwt = "expired-access";

        when(jwtService.getJwtFromCookies(request)).thenReturn(accessJwt);
        when(jwtService.getJwtRefreshFromCookies(request)).thenReturn(null);
        when(jwtService.extractUsername(accessJwt)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(response.getHeaders(HttpHeaders.SET_COOKIE)).isEmpty();
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void throwsOnUnknownRefreshToken() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/groups");
        var response = new MockHttpServletResponse();
        var accessJwt = "expired-access";
        var refreshToken = "refresh-hack";

        when(jwtService.getJwtFromCookies(request)).thenReturn(accessJwt);
        when(jwtService.getJwtRefreshFromCookies(request)).thenReturn(refreshToken);
        when(jwtService.extractUsername(accessJwt)).thenThrow(new ExpiredJwtException(null, null, "expired"));
        when(refreshTokenService.getRefreshTokenOrThrow(refreshToken)).thenThrow(new RefreshTokenException(
                ErrorCode.AUTH_REFRESH_TOKEN_EXPIRED,
                "Refresh token expired")
        );

        assertThrows(RefreshTokenException.class, () -> jwtFilter.doFilter(request, response, filterChain));
    }
}
