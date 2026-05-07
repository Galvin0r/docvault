package com.pw.docvault.controller;

import com.pw.docvault.entity.User;
import com.pw.docvault.model.security.UserInfo;
import com.pw.docvault.service.security.CredentialsService;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/accounts")
public class CredentialsController {

    private final CredentialsService credentialsService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

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

    @PatchMapping("/change-login")
    public ResponseEntity<UserInfo> changeLogin(@RequestParam String newLogin) {
        var newInfo = credentialsService.changeLogin(newLogin);
        return ResponseEntity.ok(newInfo);
    }

    @GetMapping
    public ResponseEntity<UserInfo> getUserInfo(@RequestParam(required = false) String username) {
        var info = credentialsService.getUserInfo(username);
        return ResponseEntity.ok().body(info);
    }
}