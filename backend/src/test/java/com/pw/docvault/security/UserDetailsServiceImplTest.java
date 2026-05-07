package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.service.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsernameReturnsUserWhenEmailExists() {
        var email = "user@example.com";
        var user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername(email);

        assertThat(result).isSameAs(user);
        verify(userRepository).findByEmail(email);
    }

    @Test
    void loadUserByUsernameThrowsNotFoundWhenEmailMissing() {
        String email = "missing@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername(email))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(email);

        verify(userRepository).findByEmail(email);
    }
}