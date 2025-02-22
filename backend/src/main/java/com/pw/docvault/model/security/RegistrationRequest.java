package com.pw.docvault.model.security;

public record RegistrationRequest(String login, String email, String password) {
}
