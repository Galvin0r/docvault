package com.pw.docvault.service.user;

import com.pw.docvault.entity.User;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserOrThrowReturnsUserWhenFound() {
        User user = new User();
        user.setId(7L);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        User result = userService.getUserOrThrow(7L);

        assertThat(result).isSameAs(user);
        verify(userRepository).findById(7L);
    }

    @Test
    void getUserOrThrowThrowsNotFoundWhenUserIsMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserOrThrow(99L))
                .isInstanceOfSatisfying(NotFoundException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    assertThat(exception).hasMessage("User with id 99 not found.");
                });

        verify(userRepository).findById(99L);
    }

    @Test
    void zReturnsUserWhenLoginExists() {
        User user = new User();
        user.setLogin("roman");

        when(userRepository.findByLogin("roman")).thenReturn(Optional.of(user));

        User result = userService.z("roman");

        assertThat(result).isSameAs(user);
        verify(userRepository).findByLogin("roman");
    }

    @Test
    void zThrowsNotFoundWhenLoginIsMissing() {
        when(userRepository.findByLogin("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.z("missing"))
                .isInstanceOfSatisfying(NotFoundException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    assertThat(exception).hasMessage("User with login missing not found.");
                });

        verify(userRepository).findByLogin("missing");
    }

    @Test
    void getUserByLoginOrEmailOrThrowReturnsUserWhenLoginExists() {
        User user = new User();
        user.setLogin("roman");

        when(userRepository.findByLogin("roman")).thenReturn(Optional.of(user));

        User result = userService.getUserByLoginOrEmailOrThrow("roman");

        assertThat(result).isSameAs(user);
        verify(userRepository).findByLogin("roman");
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void getUserByLoginOrEmailOrThrowReturnsUserWhenEmailExists() {
        User user = new User();
        user.setEmail("roman@example.com");

        when(userRepository.findByLogin("roman@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("roman@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByLoginOrEmailOrThrow("roman@example.com");

        assertThat(result).isSameAs(user);
        verify(userRepository).findByLogin("roman@example.com");
        verify(userRepository).findByEmail("roman@example.com");
    }

    @Test
    void getUserByLoginOrEmailOrThrowThrowsNotFoundWhenIdentifierIsMissing() {
        when(userRepository.findByLogin("missing")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByLoginOrEmailOrThrow("missing"))
                .isInstanceOfSatisfying(NotFoundException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    assertThat(exception).hasMessage("User with login or email missing not found.");
                });

        verify(userRepository).findByLogin("missing");
        verify(userRepository).findByEmail("missing");
    }
}