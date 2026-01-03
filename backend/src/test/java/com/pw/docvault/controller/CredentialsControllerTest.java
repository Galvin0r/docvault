package com.pw.docvault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pw.docvault.entity.User;
import com.pw.docvault.model.security.UserInfo;
import com.pw.docvault.service.security.CredentialsService;
import com.pw.docvault.service.security.JwtService;
import com.pw.docvault.service.security.RefreshTokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class CredentialsControllerTest {

    @Mock
    private CredentialsService credentialsService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;
    @BeforeEach
    void setup() {
        var controller = new CredentialsController(credentialsService, refreshTokenService, jwtService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    // logout

    @Test
    void logoutReturns200SetsThreeCookiesAndDeletesRefreshToken() throws Exception {
        var u = new User(); u.setId(42L);
        SecurityContextHolder.getContext()
                             .setAuthentication(new TestingAuthenticationToken(u, null));

        var jwt = ResponseCookie.from("jwt", "").path("/api").build();
        var refresh = ResponseCookie.from("jwt_refresh", "").path("/api").build();
        var jsession = ResponseCookie.from("JSESSIONID", "").path("/api").build();

        when(jwtService.getCleanJwtCookies()).thenReturn(jwt);
        when(jwtService.getCleanJwtRefreshCookie()).thenReturn(refresh);
        when(jwtService.getCleanJSessionIdCookie()).thenReturn(jsession);

        var res = mockMvc.perform(post("/accounts/logout").param("deviceInfo", "dev-1"))
                         .andExpect(status().isOk())
                         .andExpect(content().string(""))
                         .andReturn()
                         .getResponse();

        var cookies = res.getHeaders("Set-Cookie");
        assertThat(cookies).containsExactlyInAnyOrder(jwt.toString(), refresh.toString(), jsession.toString());
        verify(refreshTokenService).deleteByUserIdAndDeviceInfo(42L, "dev-1");
    }

    @Test
    void logoutDoesNotDeleteRefreshTokenWhenNoUser() throws Exception {
        SecurityContextHolder.getContext()
                             .setAuthentication(new TestingAuthenticationToken(null, null));

        when(jwtService.getCleanJwtCookies()).thenReturn(ResponseCookie.from("jwt", "").build());
        when(jwtService.getCleanJwtRefreshCookie()).thenReturn(ResponseCookie.from("jwt_refresh", "").build());
        when(jwtService.getCleanJSessionIdCookie()).thenReturn(ResponseCookie.from("JSESSIONID", "").build());

        mockMvc.perform(post("/accounts/logout").param("deviceInfo", "dev-x"))
               .andExpect(status().isOk());

        verify(refreshTokenService, never()).deleteByUserIdAndDeviceInfo(any(), anyString());
    }

    // changeLogin

    @Test
    void changeLoginReturns200AndDelegates() throws Exception {
        mockMvc.perform(post("/accounts/change-login").param("newLogin", "new_name"))
               .andExpect(status().isOk());

        verify(credentialsService).changeLogin("new_name");
    }

    // me

    @Test
    void getMeReturns200AndDelegates() throws Exception {
        var info = mock(UserInfo.class);
        when(credentialsService.getUserInfo("test")).thenReturn(info);

        mockMvc.perform(get("/accounts?username=test").accept(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk());

        verify(credentialsService).getUserInfo("test");
    }
}
