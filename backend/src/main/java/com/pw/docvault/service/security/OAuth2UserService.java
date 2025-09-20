package com.pw.docvault.service.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.Role;
import com.pw.docvault.model.security.RoleCode;
import com.pw.docvault.repository.RoleRepository;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.util.Constants;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public OAuth2UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, String> userInfo = extractUserInfo(oAuth2User, provider);

        String email = userInfo.get("email");
        String login = userInfo.get("login");

        if (userRepository.findByEmail(email).isEmpty()) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setLogin(login);
            newUser.setEnabled(true);
            newUser.setOauth2Provider(provider);
            Role userRole = roleRepository.findByName(RoleCode.USER.name())
                    .orElseThrow(() -> new IllegalStateException("Role USER was not initialized"));
            newUser.setRoles(List.of(userRole));
            userRepository.save(newUser);
        }

        return oAuth2User;
    }

    private Map<String, String> extractUserInfo(OAuth2User oAuth2User, String provider) {
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String)attributes.get(Constants.ATTRIBUTE_GOOGLE_EMAIL);
        String login;

        if (provider.equals(Constants.PROVIDER_GOOGLE)) {
            login = (String) attributes.get(Constants.ATTRIBUTE_GOOGLE_LOGIN);
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + provider);
        }

        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("email", email);
        userInfo.put("login", login);

        return userInfo;
    }
}
