package com.pw.docvault.handler;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.repository.RefreshTokenRepository;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import com.pw.docvault.util.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Value(value = "${app.frontend.url}")
    private String frontendUrl;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    public OAuth2AuthenticationSuccessHandler(UserRepository userRepository, JwtService jwtService, RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String provider = authToken.getAuthorizedClientRegistrationId();

        String email = "";
        if(provider.equals(Constants.PROVIDER_GOOGLE)) {
            email = oAuth2User.getAttribute(Constants.ATTRIBUTE_GOOGLE_EMAIL);
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setOauth2Provider(provider);

        String deviceInfo = request.getParameter("state");
        if (deviceInfo != null) {
            deviceInfo = URLDecoder.decode(deviceInfo, StandardCharsets.UTF_8);
        }

        String jwt = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.getRefreshToken(user, deviceInfo, true);
        refreshTokenRepository.save(refreshToken);

        ResponseCookie jwtCookie = jwtService.generateJwtCookie(jwt);
        ResponseCookie refreshCookie = jwtService.generateRefreshJwtCookie(refreshToken.getToken());

        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        response.sendRedirect(frontendUrl);
    }
}
