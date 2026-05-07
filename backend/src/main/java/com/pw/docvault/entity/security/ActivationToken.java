package com.pw.docvault.entity.security;

import com.pw.docvault.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "activation_tokens")
public class ActivationToken {

    @Id
    @SequenceGenerator(name = "ACTIVATION_TOKENS_ID_GENERATOR", sequenceName = "ACTIVATION_TOKENS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ACTIVATION_TOKENS_ID_GENERATOR")
    private Long id;

    private String token;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}