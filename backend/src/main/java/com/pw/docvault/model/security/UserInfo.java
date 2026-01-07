package com.pw.docvault.model.security;

import java.time.Instant;

public record UserInfo(String login, String email, Instant created) {
}
