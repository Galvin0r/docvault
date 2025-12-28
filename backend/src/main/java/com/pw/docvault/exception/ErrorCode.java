package com.pw.docvault.exception;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorCode {
    // document
    DOCUMENT_NOT_FOUNT("document.not_found"),
    DOCUMENT_UPLOAD_FAILED("document.upload_failed"),
    DOCUMENT_FORBIDDEN("document.forbidden"),
    DOCUMENT_INVALID_STATE("document.invalid_state"),
    DOCUMENT_EMPTY("document.empty"),

    // auth
    AUTH_BAD_CREDENTIALS("auth.bad_credentials"),
    AUTH_ACTIVATION_TOKEN_INVALID("auth.activation_token.invalid"),
    AUTH_ACTIVATION_TOKEN_EXPIRED("auth.activation_token.expired"),
    AUTH_PASSWORD_RESET_TOKEN_INVALID("auth.password_reset_token.invalid"),
    AUTH_PASSWORD_RESET_TOKEN_EXPIRED("auth.password_reset_token.expired"),
    AUTH_REFRESH_TOKEN_INVALID("auth.refresh_token.invalid"),
    AUTH_REFRESH_TOKEN_EXPIRED("auth.refresh_token.expired"),

    // group
    GROUP_NOT_FOUND("group.not_found"),
    GROUP_ACCESS_FORBIDDEN("group.access_forbidden"),

    // membership
    MEMBER_NOT_ALLOWED("membership.not_allowed"),
    MEMBER_NOT_FOUND("membership.not_found"),

    // join request
    JOIN_REQUEST_NOT_FOUND("join_request.not_found"),
    GROUP_REQUESTS_FORBIDDEN("join_request.access_forbidden"),

    // user
    USER_NOT_FOUND("user.not_found"),
    USER_EMAIL_TAKEN("user.email_taken"),
    USER_LOGIN_TAKEN("user.login_taken"),
    USER_ALREADY_ACTIVATED("user.already_activated"),
    USER_NOT_ACTIVATED("user.not_activated"),

    // generic
    UNKNOWN("unknown");

    private final String value;
    ErrorCode(String value) {
        this.value = value;
    }

    @JsonValue
    public String value() {
        return value;
    }
}
