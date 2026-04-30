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
}
