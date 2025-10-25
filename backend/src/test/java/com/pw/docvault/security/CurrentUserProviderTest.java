package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.service.security.CurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.*;

import java.util.ArrayList;

public class CurrentUserProviderTest {

    private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

    private User principal;

    @BeforeEach
    public void setUp() {
        principal = new User();
        principal.setId(123L);
        principal.setLogin("alice");
        principal.setRoles(new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, java.util.List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void get_returnsPrincipalAsUser() {
        assertThat(currentUserProvider.get()).isSameAs(principal);
    }

    @Test
    void getId_returnsPrincipalId() {
        assertThat(currentUserProvider.getId()).isEqualTo(principal.getId());
    }
}
