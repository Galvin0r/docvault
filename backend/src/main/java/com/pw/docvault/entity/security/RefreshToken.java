package com.pw.docvault.entity.security;

import com.pw.docvault.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name="refresh_tokens")
public class RefreshToken {

    @Id
    @SequenceGenerator(name = "REFRESH_TOKENS_ID_GENERATOR", sequenceName = "REFRESH_TOKENS_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "REFRESH_TOKENS_ID_GENERATOR")
    private Long id;

    private String token;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "device_info")
    private String deviceInfo;
}