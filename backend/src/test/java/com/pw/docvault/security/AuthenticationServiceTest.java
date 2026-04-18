package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.ActivationToken;
import com.pw.docvault.entity.security.PasswordResetToken;
import com.pw.docvault.entity.security.RefreshToken;
import com.pw.docvault.entity.security.Role;
import com.pw.docvault.exception.*;
import com.pw.docvault.model.enums.EmailTemplateName;
import com.pw.docvault.model.security.AuthenticationCookies;
import com.pw.docvault.model.security.AuthenticationRequest;
import com.pw.docvault.model.security.RegistrationRequest;
import com.pw.docvault.model.security.RoleCode;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.security.ActivationTokenRepository;
import com.pw.docvault.repository.security.PasswordResetTokenRepository;
import com.pw.docvault.repository.security.RoleRepository;
import com.pw.docvault.service.EmailService;
import com.pw.docvault.service.security.AuthenticationService;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import com.pw.docvault.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private final int activationTokenExpiresInSec = 900; // 15min
    private final String activationBaseUrl = "https://app.example.com/activate";
    private final String passwordResetBaseUrl = "https://app.example.com/reset";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(authenticationService, "activationTokenExpiresIn",
                                     activationTokenExpiresInSec);
        ReflectionTestUtils.setField(authenticationService, "activationUrl",
                                     activationBaseUrl);
        ReflectionTestUtils.setField(authenticationService, "passwordResetUrl",
                                     passwordResetBaseUrl);
    }

    private User user(String login, String email, Boolean enabled) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setEnabled(enabled);
        return user;
    }

    // register

    @Test
    void registerShouldPersistUserAndSendActivationEmail() throws Exception {
        var user = user("alice", "a@ex.com", false);
        var password = "pass";
        var hashedPassword = "hashed_pass";

        var userRole = new Role();
        userRole.setName(RoleCode.USER.name());
        when(roleRepository.findByName(RoleCode.USER.name()))
                .thenReturn(Optional.of(userRole));

        when(userRepository.findByEmail(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByLogin(user.getLogin())).thenReturn(Optional.empty());

        when(passwordEncoder.encode(password)).thenReturn(hashedPassword);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<String> emailArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> loginArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EmailTemplateName> templateArg = ArgumentCaptor.forClass(EmailTemplateName.class);
        ArgumentCaptor<String> urlArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenArg = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectArg = ArgumentCaptor.forClass(String.class);

        when(activationTokenRepository.save(any(ActivationToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var request = new RegistrationRequest(user.getLogin(), user.getUsername(), password);
        authenticationService.register(request);

        verify(userRepository).save(argThat(u ->
                                                    u.getEmail().equals(user.getUsername())
                                                            && u.getLogin().equals(user.getLogin())
                                                            && u.getPassword().equals(hashedPassword)
                                                            && !u.isEnabled()
                                                            && u.getRoles() != null && !u.getRoles().isEmpty()
        ));

        verify(emailService).sendEmail(
                emailArg.capture(),
                loginArg.capture(),
                templateArg.capture(),
                urlArg.capture(),
                tokenArg.capture(),
                subjectArg.capture()
        );

        assertThat(emailArg.getValue()).isEqualTo(user.getUsername());
        assertThat(loginArg.getValue()).isEqualTo(user.getLogin());
        assertThat(templateArg.getValue()).isEqualTo(EmailTemplateName.ACTIVATE_ACCOUNT);
        assertThat(subjectArg.getValue()).containsIgnoringCase("activation");

        assertThat(tokenArg.getValue()).matches("\\d{6}");

        String expectedUrl = UriComponentsBuilder.fromUriString(activationBaseUrl)
                                    .queryParam("email", user.getUsername())
                                    .build().toUriString();
        assertThat(urlArg.getValue()).isEqualTo(expectedUrl);
    }


    @Test
    void registerShouldFailWhenRoleMissing() {
        when(roleRepository.findByName(RoleCode.USER.name())).thenReturn(Optional.empty());
        var request = new RegistrationRequest("alice", "a@ex.com", "pass");
        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Role USER");
    }

    @Test
    void registerShouldFailWhenEmailTaken() {
        when(roleRepository.findByName(RoleCode.USER.name()))
                .thenReturn(Optional.of(new Role()));
        when(userRepository.findByEmail("a@ex.com")).thenReturn(Optional.of(new User()));

        var request = new RegistrationRequest("alice", "a@ex.com", "pw");
        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("email");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_shouldFailWhenLoginTaken() {
        when(roleRepository.findByName(RoleCode.USER.name()))
                .thenReturn(Optional.of(new Role()));
        when(userRepository.findByEmail("a@ex.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("alice")).thenReturn(Optional.of(new User()));

        var request = new RegistrationRequest("alice", "a@ex.com", "pw");
        assertThatThrownBy(() -> authenticationService.register(request))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("login");
        verify(userRepository, never()).save(any());
    }

    // authenticate

    @Test
    void authenticateWithEmailShouldReturnCookies() {
        var enabledUser = user(null, "bob@ex.com", true);

        when(userRepository.findByEmail(enabledUser.getUsername())).thenReturn(Optional.of(enabledUser));

        Authentication springAuth = new UsernamePasswordAuthenticationToken(enabledUser, null, List.of());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(springAuth);

        ResponseCookie accessCookie = ResponseCookie.from("jwt", "access").path("/api").httpOnly(true).build();
        when(jwtService.generateJwtCookie(enabledUser)).thenReturn(accessCookie);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("r1");
        when(refreshTokenService.getRefreshToken(any(User.class), any(), anyBoolean()))
                .thenReturn(refreshToken);

        ResponseCookie refreshCookie = ResponseCookie.from("jwt_refresh", "r1").path("/api").httpOnly(true).build();
        when(jwtService.generateRefreshJwtCookie("r1")).thenReturn(refreshCookie);

        AuthenticationRequest request = new AuthenticationRequest(enabledUser.getUsername(), "pass", "pw", "info", false);

        AuthenticationCookies cookies = authenticationService.authenticate(request);

        assertThat(cookies.jwtCookie().getValue()).isEqualTo("access");
        assertThat(cookies.jwtRefreshCookie().getValue()).isEqualTo("r1");
    }

    @Test
    void authenticateWithLoginShouldReturnCookies() {
        User enabledUser = user("alice", "bob@ex.com", true);

        when(userRepository.getEmailByLogin(enabledUser.getLogin())).thenReturn(Optional.of(enabledUser.getUsername()));
        when(userRepository.findByEmail(enabledUser.getUsername())).thenReturn(Optional.of(enabledUser));

        Authentication springAuth = new UsernamePasswordAuthenticationToken(enabledUser, null, List.of());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(springAuth);

        ResponseCookie accessCookie = ResponseCookie.from("jwt", "access").path("/api").httpOnly(true).build();
        when(jwtService.generateJwtCookie(enabledUser)).thenReturn(accessCookie);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("r1");
        when(refreshTokenService.getRefreshToken(any(User.class), any(), anyBoolean()))
                .thenReturn(refreshToken);

        ResponseCookie refreshCookie = ResponseCookie.from("jwt_refresh", "r1").path("/api").httpOnly(true).build();
        when(jwtService.generateRefreshJwtCookie("r1")).thenReturn(refreshCookie);

        AuthenticationRequest request = new AuthenticationRequest(null, "pass", enabledUser.getLogin(), "info", false);

        AuthenticationCookies cookies = authenticationService.authenticate(request);

        assertThat(cookies.jwtCookie().getValue()).isEqualTo("access");
        assertThat(cookies.jwtRefreshCookie().getValue()).isEqualTo("r1");
    }

    @Test
    void authenticateShouldFailWhenLoginUnknown() {
        var login = "ghost";
        when(userRepository.getEmailByLogin(login)).thenReturn(Optional.empty());
        AuthenticationRequest request = new AuthenticationRequest(null, null, login, null, false);
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email/login or password");
    }

    @Test
    void authenticateShouldFailOnBadCredentials() {
        var enabledUser = user(null, "e@ex.com", true);
        when(userRepository.findByEmail(enabledUser.getUsername())).thenReturn(Optional.of(enabledUser));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("bad creds"));

        AuthenticationRequest request = new AuthenticationRequest(enabledUser.getUsername(), null, null, null, false);
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email/login or password");
    }

    @Test
    void authenticateShouldFailWhenUserDisabled() {
        var disabledUser = user("pw", "d@ex.com", false);
        when(userRepository.findByEmail(disabledUser.getUsername())).thenReturn(Optional.of(disabledUser));

        AuthenticationRequest request = new AuthenticationRequest(disabledUser.getUsername(), disabledUser.getUsername(),
                                                                  disabledUser.getLogin(), null, false);
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not activated");
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    // activate account

    @Test
    void activateAccountSuccessMarksUserEnabledAndTokenValidated() {
        var user = user(null, null, false);
        user.setId(7L);

        ActivationToken token = new ActivationToken();
        token.setToken("123456");
        token.setUser(user);
        token.setExpiresAt(Instant.now().plusSeconds(300));

        when(activationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));
        when(userService.getUserOrThrow(user.getId())).thenReturn(user);

        authenticationService.activateAccount(token.getToken());

        assertThat(user.isEnabled()).isTrue();
        assertThat(token.getValidatedAt()).isNotNull();
    }

    @Test
    void activateAccountShouldFailWhenTokenMissing() {
        when(activationTokenRepository.findByToken("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authenticationService.activateAccount("missing"))
                .isInstanceOf(ActivationTokenException.class);
    }

    @Test
    void activateAccountShouldFailWhenTokenExpired() {
        var user = user(null, null, false);
        user.setId(1L);

        ActivationToken expired = new ActivationToken();
        expired.setUser(user);
        expired.setExpiresAt(Instant.now().minusSeconds(1));

        when(activationTokenRepository.findByToken("expired")).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> authenticationService.activateAccount("expired"))
                .isInstanceOf(ActivationTokenException.class)
                .hasMessageContaining("expired");
    }

    // resend activation token

    @Test
    void resendActivationTokenDeletesOldAndSendsNewWhenUserNotEnabled() throws Exception {
        var user = user("u", "u@ex.com", false);
        when(userRepository.findByEmail(user.getUsername())).thenReturn(Optional.of(user));
        when(activationTokenRepository.save(any(ActivationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        authenticationService.resendActivationToken(user.getUsername());

        verify(activationTokenRepository).deleteByUser(user);
        verify(emailService).sendEmail(eq(user.getUsername()), eq(user.getLogin()),
                                       eq(EmailTemplateName.ACTIVATE_ACCOUNT), anyString(), anyString(),
                                       contains("activation"));
    }

    @Test
    void resendActivationTokenShouldFailWhenUserNotFound() {
        var email = "none@ex.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authenticationService.resendActivationToken(email))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void resendActivationTokenShouldFailWhenUserAlreadyEnabled() {
        var email = "e@ex.com";
        User enabled = new User(); enabled.setEmail(email); enabled.setEnabled(true);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(enabled));
        assertThatThrownBy(() -> authenticationService.resendActivationToken(email))
                .isInstanceOf(ConflictException.class);
    }

    // initiate password reset

    @Test
    void initiatePasswordResetSendsEmailAndStoresTokenWhenUserEnabled() throws Exception {
        var user = user("p", "p@ex.com", true);

        when(userRepository.findByEmail(user.getUsername())).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        authenticationService.initiatePasswordReset(user.getUsername());

        verify(passwordResetTokenRepository).deleteByUser(user);

        ArgumentCaptor<String> resetUrlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(eq(user.getUsername()), eq(user.getLogin()),
                                       eq(EmailTemplateName.RESET_PASSWORD),
                                       resetUrlCaptor.capture(), anyString(), contains("reset"));
        assertThat(resetUrlCaptor.getValue()).startsWith(passwordResetBaseUrl).contains("?token=");
    }

    @Test
    void initiatePasswordResetShouldFailWhenUserMissing() {
        var email = "x@ex.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authenticationService.initiatePasswordReset(email))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void initiatePasswordResetShouldFailWhenUserDisabled() {
        var disabledUser = user("y","y@ex.com", false);
        when(userRepository.findByEmail(disabledUser.getUsername())).thenReturn(Optional.of(disabledUser));
        assertThatThrownBy(() -> authenticationService.initiatePasswordReset(disabledUser.getUsername()))
                .isInstanceOf(ConflictException.class);
    }

    // set new password

    @Test
    void setNewPasswordSuccessEncodesAndDeletesToken() {
        var user = new User();

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUser(user);
        prt.setToken("t1");
        prt.setExpiresAt(Instant.now().plusSeconds(60));

        var newPassword = "new_pass";
        var hashPassword = "pass_hash";

        when(passwordResetTokenRepository.findByToken(prt.getToken())).thenReturn(Optional.of(prt));
        when(passwordEncoder.encode(newPassword)).thenReturn(hashPassword);

        authenticationService.setNewPassword(prt.getToken(), newPassword);

        assertThat(user.getPassword()).isEqualTo(hashPassword);
        verify(passwordResetTokenRepository).deleteByUser(user);
    }

    @Test
    void setNewPasswordShouldFailWhenTokenMissing() {
        var token = "missing";
        when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authenticationService.setNewPassword(token, "pass"))
                .isInstanceOf(PasswordResetTokenException.class);
    }

    @Test
    void setNewPasswordShouldFailWhenTokenExpired() {
        PasswordResetToken expired = new PasswordResetToken();
        expired.setToken("expired");
        expired.setUser(new User());
        expired.setExpiresAt(Instant.now().minusSeconds(1));
        when(passwordResetTokenRepository.findByToken(expired.getToken())).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> authenticationService.setNewPassword(expired.getToken(), "pass"))
                .isInstanceOf(PasswordResetTokenException.class)
                .hasMessageContaining(expired.getToken());
    }

    @Test
    void registerShouldWrapAndPropagateWhenActivationEmailFails() throws Exception {
        var req = new RegistrationRequest("alice", "a@ex.com", "pw");
        var role = new Role();
        role.setName(RoleCode.USER.name());

        when(roleRepository.findByName(RoleCode.USER.name())).thenReturn(Optional.of(role));
        when(userRepository.findByEmail("a@ex.com")).thenReturn(Optional.empty());
        when(userRepository.findByLogin("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(activationTokenRepository.save(any(ActivationToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        doThrow(new jakarta.mail.MessagingException("fail"))
                .when(emailService).sendEmail(any(), any(), any(), any(), any(), any());

        assertThatThrownBy(() -> authenticationService.register(req))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void initiatePasswordResetShouldWrapWhenEmailSendFails() throws Exception {
        var u = new User(); u.setEmail("e@ex.com"); u.setLogin("l"); u.setEnabled(true);

        when(userRepository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(i -> i.getArgument(0));

        doThrow(new RuntimeException("send-failed"))
                .when(emailService).sendEmail(any(), any(), eq(EmailTemplateName.RESET_PASSWORD),
                        any(), any(), any());

        assertThatThrownBy(() -> authenticationService.initiatePasswordReset(u.getEmail()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void authenticateShouldFailWhenEmailUnknown() {
        var email = "ghost@ex.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        var req = new AuthenticationRequest(email, "pw", null, null, false);

        assertThatThrownBy(() -> authenticationService.authenticate(req))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email/login or password");
    }
}
