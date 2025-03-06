package com.pw.docvault.service.security;

import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.entity.security.Role;
import com.pw.docvault.exception.InvalidActivationTokenException;
import com.pw.docvault.exception.UserAlreadyExistsException;
import com.pw.docvault.entity.security.ActivationToken;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.EmailTemplateName;
import com.pw.docvault.model.security.*;
import com.pw.docvault.repository.ActivationTokenRepository;
import com.pw.docvault.repository.RefreshTokenRepository;
import com.pw.docvault.repository.RoleRepository;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    @Value("${app.security.jwt.expiresInMin}")
    private int activationTokenExpiresIn;

    @Value("${app.activation.url}")
    private String activationUrl;

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final EmailService emailService;

    public AuthenticationService(RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserRepository userRepository, ActivationTokenRepository activationTokenRepository, EmailService emailService, AuthenticationManager authenticationManager, JwtService jwtService, RefreshTokenService refreshTokenService, RefreshTokenRepository refreshTokenRepository) {
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void register(RegistrationRequest request) {
        var userRole = roleRepository.findByName(RoleCode.USER.name())
                .orElseThrow(() -> new IllegalStateException("Role USER was not initialized"));
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new UserAlreadyExistsException("User with email " + request.email() + " already exists");
        }
        if (userRepository.findByLogin(request.login()).isPresent()) {
            throw new UserAlreadyExistsException("User with login " + request.login() + " already exists");
        }
        User user = new User();
        user.setRoles(List.of(userRole));
        user.setEmail(request.email());
        user.setLogin(request.login());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(false);

        userRepository.save(user);
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) {
        try {
            String newToken = generateActivationToken(user);
            emailService.sendEmail(user.getEmail(), user.getLogin(), EmailTemplateName.ACTIVATE_ACCOUNT, activationUrl, newToken, "Account activation");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private String generateActivationToken(User user) {
        String code = generateActivationCode(6);
        ActivationToken token = new ActivationToken();
        token.setToken(code);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(activationTokenExpiresIn));
        token.setUser(user);

        activationTokenRepository.save(token);
        return token.getToken();
    }

    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder(length);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            codeBuilder.append(characters.charAt(random.nextInt(characters.length())));
        }
        return codeBuilder.toString();
    }

    public AuthenticationCookies authenticate(AuthenticationRequest request) {
        String email = request.email() != null ? request.email() :
                userRepository.getEmailByLogin(request.login())
                        .orElseThrow(() -> new BadCredentialsException("Invalid email or login"));

        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        var user = (User) auth.getPrincipal();
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        ResponseCookie jwtCookie = jwtService.generateJwtCookie(user);
        RefreshToken refreshToken = refreshTokenService.getRefreshToken(user);
        ResponseCookie jwtRefreshCookie = jwtService.generateRefreshJwtCookie(refreshToken.getToken());

        return new AuthenticationCookies(jwtCookie,
                jwtRefreshCookie,
                new UserInfo(user.getLogin(), user.getEmail(), roles, user.getOauth2Provider()));
    }

    @Transactional
    public void activateAccount(String token) {
        ActivationToken savedToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidActivationTokenException("Invalid activation token"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            throw new InvalidActivationTokenException("Activation token has expired");
        }
        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new InvalidActivationTokenException("User not found"));
        user.setEnabled(true);
        savedToken.setValidatedAt(LocalDateTime.now());
    }

    @Transactional
    public void resendActivationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidActivationTokenException("User not found"));

        if (user.isEnabled()) {
            throw new IllegalStateException("Account is already activated");
        }

        activationTokenRepository.deleteByUser(user);
        sendValidationEmail(user);
    }
}
