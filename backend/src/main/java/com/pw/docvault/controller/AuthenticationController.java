package com.pw.docvault.controller;

import com.pw.docvault.model.security.AuthenticationRequest;
import com.pw.docvault.model.security.AuthenticationCookies;
import com.pw.docvault.model.security.RegistrationRequest;
import com.pw.docvault.service.security.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegistrationRequest request) {
        authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        AuthenticationCookies response = authenticationService.authenticate(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, response.jwtCookie().toString())
                .header(HttpHeaders.SET_COOKIE, response.jwtRefreshCookie().toString())
                .body(response.userInfo());
    }

    @PostMapping("/activateAccount")
    public ResponseEntity<?> activateAccount(@RequestParam String token) {
        authenticationService.activateAccount(token);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resendActivation")
    public ResponseEntity<?> resendActivationToken(@RequestParam String email) {
        authenticationService.resendActivationToken(email);
        return ResponseEntity.accepted().build();
    }
}
