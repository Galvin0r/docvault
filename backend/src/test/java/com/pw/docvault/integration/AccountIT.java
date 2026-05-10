package com.pw.docvault.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor
class AccountIT extends AbstractWebIntegrationIT {

    private final IntegrationSupport support;

    @BeforeEach
    void resetState() {
        support.resetState();
    }

    @Test
    void accountLoginChangePersistsAndRejectsDuplicates() throws Exception {
        var alice = support.createUser("alice");
        var bob = support.createUser("bob");

        support.mockMvc().perform(get("/accounts")
                        .with(user(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("alice"));

        support.mockMvc().perform(patch("/accounts/change-login")
                        .with(user(alice))
                        .param("newLogin", "alicia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("alicia"));

        support.mockMvc().perform(get("/accounts")
                        .with(user(bob))
                        .param("username", "alicia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("alicia"))
                .andExpect(jsonPath("$.email").value("alice@example.test"));

        support.mockMvc().perform(patch("/accounts/change-login")
                        .with(user(alice))
                        .param("newLogin", "bob"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("user.login_taken"));
    }

    @Test
    void anonymousUsersCannotUseAccountEndpoints() throws Exception {
        support.mockMvc().perform(get("/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void lookupCurrentUserAndMissingUsersThroughAccountEndpoint() throws Exception {
        var alice = support.createUser("alice");

        support.mockMvc().perform(get("/accounts")
                        .with(user(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("alice"))
                .andExpect(jsonPath("$.email").value("alice@example.test"));

        support.mockMvc().perform(get("/accounts")
                        .with(user(alice))
                        .param("username", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("user.not_found"));
    }

    @Test
    void logoutClearsAuthenticationCookies() throws Exception {
        var alice = support.createUser("alice");

        support.mockMvc().perform(post("/accounts/logout")
                        .with(user(alice))
                        .param("deviceInfo", "browser-a"))
                .andExpect(status().isOk())
                .andExpect(header().stringValues("Set-Cookie",
                        "docvault-jwt=; Path=/api",
                        "docvault-jwt-refresh=; Path=/api",
                        "JSESSIONID=; Path=/api"));
    }
}
