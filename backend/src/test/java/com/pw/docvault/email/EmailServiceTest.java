package com.pw.docvault.email;

import com.pw.docvault.model.enums.EmailTemplateName;
import com.pw.docvault.service.EmailService;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage message;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(emailService, "from", "noreply@docvault.test");

        message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
    }

    private static String extractBody(MimeMessage msg) throws Exception {
        return extractFrom(msg.getContent());
    }

    private static String extractFrom(Object content) throws Exception {

        if (content instanceof String s) {
            return s;
        }

        if (content instanceof Multipart mp) {
            for (int i = 0; i < mp.getCount(); i++) {
                var part = mp.getBodyPart(i);
                if (part.isMimeType("text/html")) {
                    return part.getContent().toString();
                }
            }

            for (int i = 0; i < mp.getCount(); i++) {
                var nested = extractFrom(mp.getBodyPart(i).getContent());
                if (nested != null) return nested;
            }
        }
        return null;
    }

    @Test
    void sendEmail_usesDefaultTemplateWhenNull() throws Exception {
        var expectedHtml = "<h1>hi</h1>";
        when(templateEngine.process(eq("confirm-email"), any(Context.class))).thenReturn(expectedHtml);

        emailService.sendEmail(
                "user@ex.com",
                "Alice",
                null,
                "https://ex/confirm",
                "123456",
                "Subject"
        );

        ArgumentCaptor<Context> ctxCap = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("confirm-email"), ctxCap.capture());
        Map<String, Object> vars = ctxCap.getValue().getVariableNames().stream()
                                         .collect(java.util.stream.Collectors.toMap(n -> n, n -> ctxCap.getValue().getVariable(n)));
        assertThat(vars)
                .containsEntry("username", "Alice")
                .containsEntry("confirmationUrl", "https://ex/confirm")
                .containsEntry("code", "123456");

        assertThat(message.getFrom()[0].toString()).contains("noreply@docvault.test");
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@ex.com");
        assertThat(message.getSubject()).isEqualTo("Subject");

        String body = extractBody(message);
        assertThat(body).isNotNull();
        assertThat(body).contains(expectedHtml);

        verify(mailSender).send(message);
    }

    @Test
    void sendEmail_usesExplicitTemplateName() throws Exception {
        var expectedHtml = "<p>activate</p>";
        when(templateEngine.process(eq(EmailTemplateName.ACTIVATE_ACCOUNT.getName()), any(Context.class)))
                .thenReturn(expectedHtml);

        emailService.sendEmail(
                "user@ex.com",
                "Bob",
                EmailTemplateName.ACTIVATE_ACCOUNT,
                "https://ex/activate",
                "654321",
                "Activate"
        );

        verify(templateEngine).process(eq(EmailTemplateName.ACTIVATE_ACCOUNT.getName()), any(Context.class));
        assertThat(message.getSubject()).isEqualTo("Activate");

        String body = extractBody(message);
        assertThat(body).isNotNull();
        assertThat(body).contains(expectedHtml);

        verify(mailSender).send(message);
    }
}
