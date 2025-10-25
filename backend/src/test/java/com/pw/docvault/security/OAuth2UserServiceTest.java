package com.pw.docvault.security;

import com.pw.docvault.entity.User;
import com.pw.docvault.entity.security.Role;
import com.pw.docvault.model.security.RoleCode;
import com.pw.docvault.repository.UserRepository;
import com.pw.docvault.repository.security.RoleRepository;
import com.pw.docvault.service.security.OAuth2UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OAuth2UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private DefaultOAuth2UserService delegate;

    @InjectMocks
    private OAuth2UserService oAuth2UserService;

    private OAuth2User fakeGoogleUser(String email, String login) {
        Map<String,Object> attrs = Map.of(
                "email", email,
                "name", login
        );
        return new DefaultOAuth2User(Collections.emptyList(), attrs, "email");
    }

    private OAuth2UserRequest request(String providerId) {
        ClientRegistration reg = ClientRegistration
                .withRegistrationId(providerId)
                .clientId("id").clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid","email","profile")
                .authorizationUri("https://auth")
                .tokenUri("https://token")
                .userInfoUri("https://userinfo")
                .userNameAttributeName("email")
                .clientName(providerId)
                .build();
        return new OAuth2UserRequest(reg, new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "t", null, null
        ));
    }

    @Test
    void loadUserCreatesUserWhenEmailNotFound() {
        var email = "u@ex.com";
        var name = "alice";
        OAuth2User oAuth2User = fakeGoogleUser(email, name);
        when(delegate.loadUser(any())).thenReturn(oAuth2User);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        Role userRole = new Role(); userRole.setName(RoleCode.USER.name());
        when(roleRepository.findByName(RoleCode.USER.name())).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        oAuth2UserService.loadUser(request("google"));

        verify(userRepository).save(argThat(saved ->
                                                    saved.isEnabled()
                                                            && email.equals(saved.getEmail())
                                                            && name.equals(saved.getLogin())
                                                            && "google".equals(saved.getOauth2Provider())
                                                            && saved.getRoles() != null && !saved.getRoles().isEmpty()
        ));
    }

    @Test
    void loadUserDoesNotCreateWhenEmailExists() {
        var email = "exists@ex.com";
        OAuth2User oAuth2User = fakeGoogleUser(email, "any");
        when(delegate.loadUser(any())).thenReturn(oAuth2User);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(new User()));

        oAuth2UserService.loadUser(request("google"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserThrowsWhenUserRoleMissing() {
        var email = "u@ex.com";
        OAuth2User oAuth2User = fakeGoogleUser(email, "alice");
        when(delegate.loadUser(any())).thenReturn(oAuth2User);
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(roleRepository.findByName(RoleCode.USER.name())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> oAuth2UserService.loadUser(request("google")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Role USER");
    }

    @Test
    void loadUserThrowsForUnsupportedProvider() {
        when(delegate.loadUser(any())).thenReturn(fakeGoogleUser("u@ex.com", "alice"));

        assertThatThrownBy(() -> oAuth2UserService.loadUser(request("github")))
                .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class)
                .satisfies(ex -> {
                    var oae = (org.springframework.security.oauth2.core.OAuth2AuthenticationException) ex;
                    assertThat(oae.getError().getErrorCode())
                            .isEqualTo("Unsupported provider: github");
                });

        verifyNoInteractions(userRepository, roleRepository);
    }
}
