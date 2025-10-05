package com.pw.docvault.controller;

import com.pw.docvault.entity.User;
import com.pw.docvault.model.security.UserInfo;
import com.pw.docvault.service.security.CredentialsService;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounts")
public class CredentialsController {

    private final CredentialsService credentialsService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    public CredentialsController(CredentialsService credentialsService, RefreshTokenService refreshTokenService,
                                 JwtService jwtService) {
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
        ResponseCookie jSessionIdCookie = jwtService.getCleanJSessionIdCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, jwtCookie.toString())
                .header(HttpHeaders.SET_COOKIE, jwtRefreshCookie.toString())
                .header(HttpHeaders.SET_COOKIE, jSessionIdCookie.toString())
                .build();
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

    @GetMapping("/me")
    public ResponseEntity<UserInfo> getMe() {
        UserInfo info = credentialsService.getUserInfo();
        return ResponseEntity.ok().body(info);
    }
}
