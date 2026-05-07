package com.pw.docvault.entity.security;

import com.pw.docvault.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
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
    private Instant expiresAt;
}