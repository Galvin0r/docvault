package com.pw.docvault.controller;

import com.pw.docvault.model.security.AuthenticationRequest;
import com.pw.docvault.model.security.AuthenticationCookies;
import com.pw.docvault.model.security.RegistrationRequest;
import com.pw.docvault.service.security.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegistrationRequest request) {
        authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<Void> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationCookies response = authenticationService.authenticate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.jwtCookie().toString())
                .header(HttpHeaders.SET_COOKIE, response.jwtRefreshCookie().toString())
                .build();
    }

    @PostMapping("/activateAccount")
    public ResponseEntity<Void> activateAccount(@RequestParam String token) {
        authenticationService.activateAccount(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resendActivation")
    public ResponseEntity<Void> resendActivationToken(@RequestParam String email) {
        authenticationService.resendActivationToken(email);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/resetPassword")
    public ResponseEntity<Void> resetPassword(@RequestParam String email) {
        authenticationService.initiatePasswordReset(email);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/setNewPassword")
    public ResponseEntity<Void> setNewPassword(@RequestParam String token, @RequestParam String password) {
        authenticationService.setNewPassword(token, password);
        return ResponseEntity.accepted().build();
    }
}
