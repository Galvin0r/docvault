package com.pw.docvault.entity.security;

import com.pw.docvault.entity.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="refresh_tokens")
public class RefreshToken {

    @Id
    @SequenceGenerator(name = "REFRESH_TOKENS_ID_GENERATOR", sequenceName = "REFRESH_TOKENS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REFRESH_TOKENS_ID_GENERATOR")
    private Long id;

    private String token;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    public RefreshToken() {}

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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
