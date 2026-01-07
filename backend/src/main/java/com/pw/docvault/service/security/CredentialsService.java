package com.pw.docvault.service.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.exception.AlreadyExistsException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.exception.NotFoundException;
import com.pw.docvault.model.security.UserInfo;
import com.pw.docvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CredentialsService {

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUser;

    public UserInfo changeLogin(String newLogin) {
        var user = currentUser.get();
        if (userRepository.findByLogin(newLogin).isPresent()) {
            throw new AlreadyExistsException(ErrorCode.USER_LOGIN_TAKEN, "User with login " + newLogin + " already exists");
        }
        user.setLogin(newLogin);
        userRepository.save(user);

        return new UserInfo(user.getLogin(), user.getEmail(), user.getCreated());
    }

    public UserInfo getUserInfo(String username) {
        User user;
        if (username == null || username.isEmpty()) {
            user = currentUser.get();
        } else {
            user = userRepository.findByLogin(username).orElseThrow(
                    () -> new NotFoundException(ErrorCode.USER_NOT_FOUND, "User with login " + username +  " not found"));
        }

        return new UserInfo(user.getLogin(), user.getEmail(), user.getCreated());
    }
}
