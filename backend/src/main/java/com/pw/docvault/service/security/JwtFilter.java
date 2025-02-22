package com.pw.docvault.service.security;

import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.JwtTokenException;
import com.pw.docvault.exception.TokenRefreshException;
import com.pw.docvault.repository.RefreshTokenRepository;
import com.pw.docvault.service.UserDetailsServiceImpl;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Service
public class JwtFilter extends OncePerRequestFilter {

    @Value(value = "${app.security.jwtCookieName}")
    private String jwtCookieName;

    @Value(value = "${app.security.cookieMaxAge}")
    private int cookieMaxAge;

    Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenService refreshTokenService;

    public JwtFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsServiceImpl, RefreshTokenRepository refreshTokenRepository, RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.userDetailsServiceImpl = userDetailsServiceImpl;
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = jwtService.getJwtFromCookies(request);
        String refreshToken = jwtService.getJwtRefreshFromCookies(request);

        try {
            if (jwt != null) {
                String userEmail = jwtService.extractUsername(jwt);
                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(userEmail);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        setAuthentication(userDetails, request);
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            if (refreshToken != null) {
                RefreshToken rt = refreshTokenRepository.findByToken(refreshToken)
                        .map(refreshTokenService::verifyExpiration)
                        .orElseThrow(() -> new TokenRefreshException("Refresh token expired"));

                if (rt != null) {
                    UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(rt.getUser().getEmail());

                    String newJwt = jwtService.generateToken(userDetails);
                    Cookie jwtCookie = new Cookie(jwtCookieName, newJwt);
                    jwtCookie.setHttpOnly(true);
                    jwtCookie.setPath("/api");
                    jwtCookie.setMaxAge(cookieMaxAge);
                    response.addCookie(jwtCookie);

                    setAuthentication(userDetails, request);
                } else {
                    logger.error("Refresh token not found");
                }
            } else {
                logger.error("JWT expired. No refresh token available.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
