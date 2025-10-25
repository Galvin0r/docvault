package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.exception.AlreadyExistsException;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.service.security.CredentialsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CredentialsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CredentialsService credentialsService;

    private User authenticatedUser;

    @BeforeEach
    public void setUp() {
        credentialsService = new CredentialsService(userRepository);

        authenticatedUser = new User();
        authenticatedUser.setId(42L);
        authenticatedUser.setLogin("currentLogin");
        authenticatedUser.setEmail("user@example.com");
        authenticatedUser.setRoles(new ArrayList<>());

        var authentication =
                new UsernamePasswordAuthenticationToken(authenticatedUser, null, authenticatedUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // change login

    @Test
    void changeLoginUpdatesPrincipalAndSavesNewLogin() {
        var newLogin = "newLogin";
        when(userRepository.findByLogin(newLogin)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        credentialsService.changeLogin(newLogin);

        assertThat(authenticatedUser.getLogin()).isEqualTo(newLogin);
        verify(userRepository).save(argThat(saved ->
                                                    saved.getId().equals(authenticatedUser.getId())
                                                                        && saved.getLogin().equals(newLogin)
        ));
    }

    @Test
    void changeLoginThrowsAlreadyExistsWhenLoginTaken() {
        var newLogin = "taken";
        when(userRepository.findByLogin(newLogin)).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> credentialsService.changeLogin(newLogin))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
        assertThat(authenticatedUser.getLogin()).isEqualTo("currentLogin");
    }

    // get user info
    @Test
    void getUserInfo_returnsDataFromCurrentPrincipal() {
        var info = credentialsService.getUserInfo();

        assertThat(info.login()).isEqualTo(authenticatedUser.getLogin());
        assertThat(info.email()).isEqualTo(authenticatedUser.getUsername());
        verifyNoInteractions(userRepository);
    }
}
