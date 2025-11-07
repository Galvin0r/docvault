package com.pw.docvault.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomAuthorizationRequestResolverTest {

    @Mock
    private ClientRegistrationRepository clientRegistrationRepository;

    @Mock
    private OAuth2AuthorizationRequestResolver inner;

    @InjectMocks
    private CustomAuthorizationRequestResolver resolver;

    private static final String AUTH_URI = "https://auth.example/authorize";
    private static final String CLIENT_ID = "client-id";
    private static final String REDIRECT_URI = "https://app.example/login/oauth2/code/google";
    private static final String REGISTRATION_ID = "registration_id";
    private static final String GOOGLE = "google";
    private static final String LOGIN_HINT = "login_hint";
    private static final String EMAIL = "alice@example.com";
    private static final String PROMPT = "prompt";
    private static final String SELECT_ACCOUNT = "select_account";

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(resolver, "defaultResolver", inner);
    }

    private OAuth2AuthorizationRequest baseRequest(Map<String, Object> attributes,
                                                   Map<String, Object> additionalParameters) {
        OAuth2AuthorizationRequest.Builder builder = OAuth2AuthorizationRequest.authorizationCode()
                                                                               .authorizationUri(AUTH_URI)
                                                                               .clientId(CLIENT_ID)
                                                                               .redirectUri(REDIRECT_URI);
        if (attributes != null) {
            builder.attributes(attrs -> attrs.putAll(attributes));
        }
        if (additionalParameters != null) {
            builder.additionalParameters(additionalParameters);
        }
        return builder.build();
    }

    @Test
    void resolve_returnsNull_whenInnerReturnsNull() {
        HttpServletRequest request = new MockHttpServletRequest();
        when(inner.resolve(request)).thenReturn(null);

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertThat(result).isNull();
    }

    @Test
    void resolve_addsPrompt_whenRegistrationIdPresent_andPreservesExistingAdditionalParams() {
        HttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> attributes = Map.of(REGISTRATION_ID, GOOGLE);
        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.put(LOGIN_HINT, EMAIL);
        OAuth2AuthorizationRequest resolvedByInner = baseRequest(attributes, additionalParameters);

        when(inner.resolve(request)).thenReturn(resolvedByInner);

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertThat(result).isNotNull();
        assertThat(result.getAdditionalParameters())
                .containsEntry(LOGIN_HINT, EMAIL)
                .containsEntry(PROMPT, SELECT_ACCOUNT);
        assertThat(result).isNotSameAs(resolvedByInner);
    }

    @Test
    void resolve_doesNotAddPrompt_whenRegistrationIdMissing() {
        HttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> additionalParameters = Map.of("foo", "bar");
        OAuth2AuthorizationRequest resolvedByInner = baseRequest(Map.of(), additionalParameters);

        when(inner.resolve(request)).thenReturn(resolvedByInner);

        OAuth2AuthorizationRequest result = resolver.resolve(request);

        assertThat(result).isNotNull();
        assertThat(result.getAdditionalParameters())
                .containsEntry("foo", "bar")
                .doesNotContainKey(PROMPT);
    }

    @Test
    void resolveWithClientId_callsInner_andAppliesSameCustomization() {
        HttpServletRequest request = new MockHttpServletRequest();
        Map<String, Object> attributes = Map.of(REGISTRATION_ID, GOOGLE);
        OAuth2AuthorizationRequest resolvedByInner = baseRequest(attributes, Map.of());

        when(inner.resolve(eq(request), eq(GOOGLE))).thenReturn(resolvedByInner);

        OAuth2AuthorizationRequest result = resolver.resolve(request, GOOGLE);

        assertThat(result).isNotNull();
        assertThat(result.getAdditionalParameters()).containsEntry(PROMPT, SELECT_ACCOUNT);
        verify(inner).resolve(eq(request), eq(GOOGLE));
    }
}
