package com.pw.docvault.controller;

import com.pw.docvault.entity.User;
import com.pw.docvault.service.security.CredentialsService;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
public class CredentialsController {

    private final CredentialsService credentialsService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public CredentialsController(CredentialsService credentialsService, RefreshTokenService refreshTokenService, JwtService jwtService) {
        this.credentialsService = credentialsService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(String deviceInfo) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user != null) {
            Long userId = user.getId();
            refreshTokenService.deleteByUserIdAndDeviceInfo(userId, deviceInfo);
        }

        ResponseCookie jwtCookie = jwtService.getCleanJwtCookies();
        ResponseCookie jwtRefreshCookie = jwtService.getCleanJwtRefreshCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                .body("You've been signed out!");
    }

    @PostMapping("/change-login")
    public ResponseEntity<Void> changeLogin(@RequestParam String newLogin) {
        credentialsService.changeLogin(newLogin);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/change-email")
    public ResponseEntity<?> changeEmail(@RequestParam String newEmail) {
        //TODO
        return ResponseEntity.ok().build();
    }
}
