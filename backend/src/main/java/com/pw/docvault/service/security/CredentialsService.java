package com.pw.docvault.service.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
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
            throw new BadCredentialsException("Login already taken");
        }
        user.setLogin(newLogin);
        userRepository.save(user);
    }
}
