package com.pw.docvault.service.security;

import com.pw.docvault.entity.security.RefreshToken;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Service
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final RefreshTokenService refreshTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = jwtService.getJwtFromCookies(request);
        String refreshToken = jwtService.getJwtRefreshFromCookies(request);

        try {
            if (jwt != null) {
                String userLogin = jwtService.extractUsername(jwt);
                if (userLogin != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(userLogin);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        setAuthentication(userDetails, request);
                    }
                }
            }
        } catch (ExpiredJwtException e) {
            if (refreshToken != null) {
                RefreshToken rt = refreshTokenService.getRefreshTokenOrThrow(refreshToken);

                UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(rt.getUser().getEmail());

                String newJwt = jwtService.generateToken(userDetails);
                RefreshToken newRt = refreshTokenService.rotateToken(rt.getId());
                ResponseCookie jwtCookie = jwtService.generateJwtCookie(newJwt);
                ResponseCookie refreshCookie = jwtService.generateRefreshJwtCookie(newRt.getToken());
                response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
                response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

                setAuthentication(userDetails, request);
            } else {
                log.warn("JWT expired. No refresh token available.");
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