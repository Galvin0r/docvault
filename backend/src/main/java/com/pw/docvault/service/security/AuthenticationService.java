package com.pw.docvault.service.security;

import com.pw.docvault.entity.security.PasswordResetToken;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.exception.*;
import com.pw.docvault.entity.security.ActivationToken;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.enums.EmailTemplateName;
import com.pw.docvault.model.security.*;
import com.pw.docvault.repository.security.ActivationTokenRepository;
import com.pw.docvault.repository.security.PasswordResetTokenRepository;
import com.pw.docvault.repository.security.RoleRepository;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.service.EmailService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthenticationService {

    @Value("${app.security.jwt.expiresInMin}")
    private int activationTokenExpiresIn;

    @Value("${app.activation.url}")
    private String activationUrl;

    @Value("${app.passwordReset.url}")
    private String passwordResetUrl;

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticationService(RoleRepository roleRepository, PasswordEncoder passwordEncoder,
                                 UserRepository userRepository, ActivationTokenRepository activationTokenRepository,
                                 EmailService emailService, AuthenticationManager authenticationManager,
                                 JwtService jwtService, RefreshTokenService refreshTokenService,
                                 PasswordResetTokenRepository passwordResetTokenRepository) {
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.activationTokenRepository = activationTokenRepository;
        this.emailService = emailService;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public void register(RegistrationRequest request) {
        var userRole = roleRepository.findByName(RoleCode.USER.name())
                .orElseThrow(() -> new IllegalStateException("Role USER was not initialized"));
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new AlreadyExistsException(ErrorCode.USER_EMAIL_TAKEN,
                                             "User with email " + request.email() + " already exists");
        }
        if (userRepository.findByLogin(request.login()).isPresent()) {
            throw new AlreadyExistsException(ErrorCode.USER_LOGIN_TAKEN,
                                             "User with login " + request.login() + " already exists");
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
            emailService.sendEmail(user.getEmail(), user.getLogin(), EmailTemplateName.ACTIVATE_ACCOUNT,
                                   buildActivationUrl(user.getEmail()), newToken, "Account activation");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPasswordResetEmail(User user) {
        try {
            String newToken = generatePasswordResetToken(user);
            emailService.sendEmail(user.getEmail(), user.getLogin(), EmailTemplateName.RESET_PASSWORD,
                                   buildPasswordResetUrl(newToken), newToken, "Password reset");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildActivationUrl(String email) {
        return UriComponentsBuilder
                .fromUriString(activationUrl)
                .queryParam("email", email)
                .build()
                .toUriString();
    }

    private String buildPasswordResetUrl(String token) {
        return UriComponentsBuilder
                .fromUriString(passwordResetUrl)
                .queryParam("token", token)
                .build()
                .toUriString();
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

    private String generatePasswordResetToken(User user) {
        String newToken = generateUUID();
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(newToken);
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(activationTokenExpiresIn));

        passwordResetTokenRepository.save(token);
        return token.getToken();
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
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
                        .orElseThrow(() -> new BadCredentialsException("Invalid email/login or password"));
        var user = userRepository.findByEmail(email).orElseThrow(
                () -> new BadCredentialsException("Invalid email/login or password"));
        if (!user.isEnabled()) {
            throw new ConflictException(ErrorCode.USER_NOT_ACTIVATED, "Account is not activated");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email/login or password");
        }


        user = (User) auth.getPrincipal();

        ResponseCookie jwtCookie = jwtService.generateJwtCookie(user);
        RefreshToken refreshToken = refreshTokenService.getRefreshToken(user, request.deviceInfo(),
                                                                        Optional.ofNullable(request.rememberMe()).orElse(false));
        ResponseCookie jwtRefreshCookie = jwtService.generateRefreshJwtCookie(refreshToken.getToken());

        return new AuthenticationCookies(jwtCookie, jwtRefreshCookie);
    }

    @Transactional
    public void activateAccount(String token) {
        ActivationToken savedToken = activationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ActivationTokenException(ErrorCode.AUTH_ACTIVATION_TOKEN_INVALID));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            throw new ActivationTokenException(ErrorCode.AUTH_ACTIVATION_TOKEN_EXPIRED,
                                               "Activation token has expired");
        }
        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
        user.setEnabled(true);
        savedToken.setValidatedAt(LocalDateTime.now());
    }

    @Transactional
    public void resendActivationToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));

        if (user.isEnabled()) {
            throw new ConflictException(ErrorCode.USER_ALREADY_ACTIVATED, "Account is already activated");
        }

        activationTokenRepository.deleteByUser(user);
        sendValidationEmail(user);
    }

    @Transactional
    public void initiatePasswordReset(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User not found"));
        if (!user.isEnabled()) {
            throw new ConflictException(ErrorCode.USER_NOT_ACTIVATED, "Account is not activated");
        }

        passwordResetTokenRepository.deleteByUser(user);
        sendPasswordResetEmail(user);
    }

    @Transactional
    public void setNewPassword(String token, String password) {
        PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token).orElseThrow(() ->
                new PasswordResetTokenException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_INVALID));
        if (LocalDateTime.now().isAfter(passwordResetToken.getExpiresAt())) {
            throw new PasswordResetTokenException(ErrorCode.AUTH_PASSWORD_RESET_TOKEN_EXPIRED,
                                                  "Password reset token has expired");
        }
        User user = passwordResetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        passwordResetTokenRepository.deleteByUser(user);
    }
}
