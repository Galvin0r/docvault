package com.pw.docvault.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.docvault.model.security.AuthenticationRequest;
import com.pw.docvault.model.security.RegistrationRequest;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.security.ActivationTokenRepository;
import com.pw.docvault.repository.security.PasswordResetTokenRepository;
import com.pw.docvault.repository.security.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class AuthIT extends AbstractWebIntegrationIT {

    private final IntegrationSupport support;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void resetState() {
        support.resetState();
    }

    @Test
    void registrationActivationAndLoginIssueCookiesAndRefreshToken() throws Exception {
        var registration = new RegistrationRequest("alice", "alice@example.test", "secret");

        support.mockMvc().perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated());

        var alice = userRepository.findByEmail("alice@example.test").orElseThrow();
        assertThat(alice.isEnabled()).isFalse();

        support.mockMvc().perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest("alice@example.test", "secret", null, "browser-a", true)
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.not_activated"));

        var token = activationTokenRepository.findAll().getFirst().getToken();
        support.mockMvc().perform(post("/auth/activateAccount")
                        .param("token", token))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest(null, "secret", "alice", "browser-a", true)
                        )))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));

        assertThat(userRepository.findByEmail("alice@example.test").orElseThrow().isEnabled()).isTrue();
        assertThat(refreshTokenRepository.findByUserIdAndDeviceInfo(alice.getId(), "browser-a")).isPresent();
    }

    @Test
    void registrationRejectsDuplicateLoginAndEmail() throws Exception {
        support.mockMvc().perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("alice", "alice@example.test", "secret")
                        )))
                .andExpect(status().isCreated());

        support.mockMvc().perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("other", "alice@example.test", "secret")
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.email_taken"));

        support.mockMvc().perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("alice", "other@example.test", "secret")
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.login_taken"));
    }

    @Test
    void passwordResetReplacesPasswordAndDeletesToken() throws Exception {
        var alice = support.createUser("alice");

        support.mockMvc().perform(post("/auth/resetPassword")
                        .param("email", alice.getEmail()))
                .andExpect(status().isAccepted());

        var token = passwordResetTokenRepository.findAll().getFirst().getToken();
        support.mockMvc().perform(post("/auth/setNewPassword")
                        .param("token", token)
                        .param("password", "new-secret"))
                .andExpect(status().isAccepted());

        assertThat(passwordResetTokenRepository.findAll()).isEmpty();
        support.mockMvc().perform(post("/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AuthenticationRequest(alice.getEmail(), "new-secret", null, "browser-b", false)
                        )))
                .andExpect(status().isOk());

        support.mockMvc().perform(post("/auth/setNewPassword")
                        .param("token", token)
                        .param("password", "ignored"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("auth.password_reset_token.invalid"));
    }

    @Test
    void resendActivationReplacesTokenAndRejectsActivatedUsers() throws Exception {
        support.mockMvc().perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationRequest("alice", "alice@example.test", "secret")
                        )))
                .andExpect(status().isCreated());

        var firstToken = activationTokenRepository.findAll().getFirst().getToken();
        support.mockMvc().perform(post("/auth/resendActivation")
                        .param("email", "alice@example.test"))
                .andExpect(status().isAccepted());

        var replacementToken = activationTokenRepository.findAll().getFirst().getToken();
        assertThat(replacementToken).isNotEqualTo(firstToken);

        support.mockMvc().perform(post("/auth/activateAccount")
                        .param("token", replacementToken))
                .andExpect(status().isNoContent());

        support.mockMvc().perform(post("/auth/resendActivation")
                        .param("email", "alice@example.test"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.already_activated"));
    }
}
