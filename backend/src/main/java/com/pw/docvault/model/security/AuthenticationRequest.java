package com.pw.docvault.model.security;

public record AuthenticationRequest(String email, String password, String login, String deviceInfo) {
}
