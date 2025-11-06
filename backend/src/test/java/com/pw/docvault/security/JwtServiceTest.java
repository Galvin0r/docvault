package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.Role;
import com.pw.docvault.service.security.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JwtServiceTest {

    private JwtService jwtService;

    private final String jwtCookieName = "jwt";
    private final String jwtRefreshCookieName = "jwt_refresh";
    private final String jSessionIdCookieName = "JSESSIONID";
    private final int cookieMaxAge = (int) Duration.ofHours(2).toSeconds();
    private final long jwtExpiration = Duration.ofMinutes(5).toMillis();

    @BeforeEach
    void setup() {
        jwtService = new JwtService();

        SecretKey key = Jwts.SIG.HS256.key().build();
        String secret = Encoders.BASE64.encode(key.getEncoded());

        ReflectionTestUtils.setField(jwtService, "jwtExpiration",  jwtExpiration);
        ReflectionTestUtils.setField(jwtService, "secretKey",  secret);
        ReflectionTestUtils.setField(jwtService, "jwtCookieName",  jwtCookieName);
        ReflectionTestUtils.setField(jwtService, "jwtRefreshCookieName",  jwtRefreshCookieName);
        ReflectionTestUtils.setField(jwtService, "cookieMaxAge",  cookieMaxAge);
        ReflectionTestUtils.setField(jwtService, "jSessionIdCookieName",  jSessionIdCookieName);
    }

    private User user(String username, String email, String ...roles) {
        var user = new User();
        user.setLogin(username);
        user.setPassword("{noop}pass");
        user.setEmail(email);
        user.setRoles(Arrays.stream(roles).map(r -> {
            var role = new Role();
            role.setName(r);
            return role;
        }).toList());
        return user;
    }

    @Test
    void getSignInKeyIsDeterministic() {
        var key1 = jwtService.getSignInKey();
        var key2 = jwtService.getSignInKey();
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void tokenAllowsEmptyAuthorities() {
        var user = user("test", null);
        String token = jwtService.buildToken(null, user, jwtExpiration);
        List<?> auth = (List<?>) jwtService.extractClaims(token).get("authorities");
        assertThat(auth).isEmpty();
    }

    @Test
    void tokenExtraClaimsWork() {
        var user = user("test", null);
        String token = jwtService.buildToken(Map.of("test_claim", "test_value"), user, jwtExpiration);
        String claim = (String) jwtService.extractClaims(token).get("test_claim");
        assertThat(claim).isEqualTo("test_value");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void tokenAuthorityWorks() {
        var user = user("test", null, "ADMIN");
        String token = jwtService.buildToken(null, user, jwtExpiration);
        @SuppressWarnings("unchecked")
        List<String> auth = (List<String>) jwtService.extractClaims(token).get("authorities");
        assertThat(auth).isNotEmpty();
        assertThat(auth).containsExactly("ADMIN");
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void tokenExpired() {
        var user = user("test", null);
        String token = jwtService.buildToken(null, user, -1);
        assertThat(jwtService.isTokenExpired(token)).isTrue();
    }

    @Test
    void tokenNotExpired() {
        var user = user("test", null);
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void extractorsWork() {
        var user = user("test", "example@gmail.com");
        String token = jwtService.buildToken(Map.of("test", 1), user, jwtExpiration);

        assertThat((int) jwtService.extractClaim(token, (claims) -> claims.get("test", Integer.class))).isEqualTo(1);
        assertThat(jwtService.extractUsername(token)).isEqualTo("example@gmail.com");
        assertThat(jwtService.extractExpiration(token)).isAfter(new Date(System.currentTimeMillis()));
    }

    @Test
    void isTokenUserCorrect() {
        var user = user("bob", "bob@gmail.com");
        var other = user("eve", "eve@gmail.com");
        var token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenUserCorrect(token, user)).isTrue();
        assertThat(jwtService.isTokenUserCorrect(token, other)).isFalse();
    }

    @Test
    void isTokenValid() {
        var user = user("carol", "carol@gmail.com");
        var tokenExpired = jwtService.buildToken(Map.of(), user, -1);
        assertThat(jwtService.isTokenValid(tokenExpired, user)).isFalse();

        var token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user("other", "eve@gmail.com"))).isFalse();
    }

    @Test
    void extractClaimsThrowsForTamperedSignature() {
        var user = user("test", null);
        var token = jwtService.generateToken(user);

        var tempered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> jwtService.extractClaims(tempered)).isInstanceOf(SignatureException.class);
    }

    @Test
    void generateJwtCookieHasFlagsAndValidTokenInside() {
        var user = user("test", "example@gmail.com");
        var cookie = jwtService.generateJwtCookie(user);

        assertThat(cookie.getName()).isEqualTo(jwtCookieName);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(cookieMaxAge);
        assertThat(jwtService.extractUsername(cookie.getValue())).isEqualTo("example@gmail.com");
    }

    @Test
    void generateRefreshJwtCookieKeepsValueAndFlags() {
        var cookie = jwtService.generateRefreshJwtCookie("REF-123");
        assertThat(cookie.getName()).isEqualTo(jwtRefreshCookieName);
        assertThat(cookie.getValue()).isEqualTo("REF-123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
    }

    @Test
    void cleanCookies_haveNullValueAndApiPath() {
        assertThat(jwtService.getCleanJwtCookies().getValue()).isEmpty();
        assertThat(jwtService.getCleanJwtRefreshCookie().getValue()).isEmpty();
        assertThat(jwtService.getCleanJSessionIdCookie().getValue()).isEmpty();
        assertThat(jwtService.getCleanJwtCookies().getPath()).isEqualTo("/api");
    }

    @Test
    void getJwtFromCookiesPresentAndAbsent() {
        var req = new MockHttpServletRequest();
        req.setCookies(new Cookie("other", "x"),
                       new Cookie(jwtCookieName, "JWT-ABC"),
                       new Cookie(jwtRefreshCookieName, "REF-XYZ"));

        assertThat(jwtService.getJwtFromCookies(req)).isEqualTo("JWT-ABC");
        assertThat(jwtService.getJwtRefreshFromCookies(req)).isEqualTo("REF-XYZ");

        var emptyReq = new MockHttpServletRequest();
        assertThat(jwtService.getJwtFromCookies(emptyReq)).isNull();
        assertThat(jwtService.getJwtRefreshFromCookies(emptyReq)).isNull();
    }

    @Test
    void buildTokenStandardClaimsOverrideExtraClaims() {
        var user = user("henry", "example.com", "ADMIN");
        var token = jwtService.buildToken(Map.of(
                "sub", "fake",
                "authorities", List.of("HACK"),
                "exp", new Date(0)
        ), user, jwtExpiration);
        var claims = jwtService.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo("example.com");
        @SuppressWarnings("unchecked")
        List<String> auth = (List<String>) claims.get("authorities");
        assertThat(auth).containsExactly("ADMIN");
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void generateJwtCookieWithValueKeepsValueAndFlags() {
        var cookie = jwtService.generateJwtCookie("ACCESS-123");

        assertThat(cookie.getName()).isEqualTo(jwtCookieName);
        assertThat(cookie.getValue()).isEqualTo("ACCESS-123");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(cookieMaxAge);
    }
}
