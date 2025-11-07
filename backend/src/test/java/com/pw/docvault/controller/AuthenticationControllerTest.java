package com.pw.docvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.docvault.model.security.AuthenticationCookies;
import com.pw.docvault.model.security.AuthenticationRequest;
import com.pw.docvault.model.security.RegistrationRequest;
import com.pw.docvault.service.security.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.MediaType;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        var controller = new AuthenticationController(authenticationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void registerReturns201AndDelegatesToService() throws Exception {
        var request = new RegistrationRequest("alice", "a@ex.com", "pw");

        mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
               .andExpect(status().isCreated())
               .andExpect(content().string(""));

        var cap = ArgumentCaptor.forClass(RegistrationRequest.class);

        verify(authenticationService).register(cap.capture());
        assertThat(cap.getValue().login()).isEqualTo("alice");
        assertThat(cap.getValue().email()).isEqualTo("a@ex.com");
        assertThat(cap.getValue().password()).isEqualTo("pw");
    }

    @Test
    void authenticateReturns200AndSetsBothCookies() throws Exception {
        var access = ResponseCookie.from("jwt", "ACCESS").path("/api").httpOnly(true).build();
        var refresh = ResponseCookie.from("jwt_refresh", "REFRESH").path("/api").httpOnly(true).build();
        when(authenticationService.authenticate(any()))
                .thenReturn(new AuthenticationCookies(access, refresh));

        var request = new AuthenticationRequest("a@ex.com", "pw", null, "device", false);

        var response = mockMvc.perform(post("/auth/authenticate")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(objectMapper.writeValueAsString(request)))
                         .andExpect(status().isOk())
                         .andExpect(content().string(""))
                         .andReturn()
                         .getResponse();

        var setCookies = response.getHeaders("Set-Cookie");
        assertThat(setCookies).containsExactlyInAnyOrder(access.toString(), refresh.toString());

        var cap = ArgumentCaptor.forClass(AuthenticationRequest.class);
        verify(authenticationService).authenticate(cap.capture());
        assertThat(cap.getValue().email()).isEqualTo(request.email());
        assertThat(cap.getValue().password()).isEqualTo(request.password());
        assertThat(cap.getValue().deviceInfo()).isEqualTo(request.deviceInfo());
        assertThat(cap.getValue().rememberMe()).isFalse();
    }

    @Test
    void activateAccountReturns204AndDelegates() throws Exception {
        mockMvc.perform(post("/auth/activateAccount").param("token", "T123"))
               .andExpect(status().isNoContent());

        verify(authenticationService).activateAccount("T123");
    }

    @Test
    void resendActivationReturns202AndDelegates() throws Exception {
        mockMvc.perform(post("/auth/resendActivation").param("email", "a@ex.com"))
               .andExpect(status().isAccepted());

        verify(authenticationService).resendActivationToken("a@ex.com");
    }

    @Test
    void resetPasswordReturns202AndDelegates() throws Exception {
        mockMvc.perform(post("/auth/resetPassword").param("email", "a@ex.com"))
               .andExpect(status().isAccepted());

        verify(authenticationService).initiatePasswordReset("a@ex.com");
    }

    @Test
    void setNewPasswordReturns202AndDelegates() throws Exception {
        mockMvc.perform(post("/auth/setNewPassword")
                                .param("token", "RTOK")
                                .param("password", "newPass"))
               .andExpect(status().isAccepted());

        verify(authenticationService).setNewPassword("RTOK", "newPass");
    }
}
