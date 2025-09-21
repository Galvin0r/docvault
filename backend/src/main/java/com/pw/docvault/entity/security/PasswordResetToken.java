package com.pw.docvault.entity.security;

import com.pw.docvault.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @SequenceGenerator(name = "PASSWORD_RESET_TOKENS_ID_GENERATOR", sequenceName = "PASSWORD_RESET_TOKENS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PASSWORD_RESET_TOKENS_ID_GENERATOR")
    private Long id;

    private String token;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
