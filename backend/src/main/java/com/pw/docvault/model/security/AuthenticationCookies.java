package com.pw.docvault.model.security;

import org.springframework.http.ResponseCookie;

public record AuthenticationCookies(ResponseCookie jwtCookie, ResponseCookie jwtRefreshCookie, UserInfo userInfo) {
}
