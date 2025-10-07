package com.pw.docvault.service.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.exception.AlreadyExistsException;
import com.pw.docvault.exception.ErrorCode;
import com.pw.docvault.model.security.UserInfo;
import com.pw.docvault.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CredentialsService {

    private final UserRepository userRepository;

    public CredentialsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void changeLogin(String newLogin) {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (userRepository.findByLogin(newLogin).isPresent()) {
            throw new AlreadyExistsException(ErrorCode.USER_LOGIN_TAKEN, "User with login " + newLogin + " already exists");
        }
        user.setLogin(newLogin);
        userRepository.save(user);
    }

    public UserInfo getUserInfo() {
        User user = (User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return new UserInfo(user.getLogin(), user.getEmail());
    }
}
