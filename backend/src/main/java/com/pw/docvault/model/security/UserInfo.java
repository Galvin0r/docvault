package com.pw.docvault.model.security;

import java.util.List;

public record UserInfo(String login, String email, List<String> roles, String oauth2Provider) {
}
