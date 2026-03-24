package com.pw.docvault.config;

import com.pw.docvault.exception.BadCredentialsException;
import com.pw.docvault.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.net.http.HttpClient;
import java.time.Duration;

@RequiredArgsConstructor
@Configuration
public class BeansConfig {

    private final UserDetailsService userDetailsService;

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                String username = authentication.getName();
                String password = authentication.getCredentials().toString();

                UserDetails user = userDetailsService.loadUserByUsername(username);
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    throw new BadCredentialsException(ErrorCode.AUTH_BAD_CREDENTIALS);
                }
                return new UsernamePasswordAuthenticationToken(user, password, user.getAuthorities());
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public HttpClient httpClient(@Value("${app.processing.http.connect-timeout-seconds}") int connectTimeoutSeconds) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                .build();
    }

    @Bean
    public com.google.cloud.storage.Storage storage(@Value("${app.gcs.credentials.location}") Resource credentials) throws java.io.IOException {
        return com.google.cloud.storage.StorageOptions.newBuilder()
                .setCredentials(com.google.auth.oauth2.ServiceAccountCredentials.fromStream(credentials.getInputStream()))
                .build()
                .getService();
    }
}
