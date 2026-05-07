package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class RefreshTokenException extends AppException {
    public RefreshTokenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public RefreshTokenException(ErrorCode errorCode) {
        super(errorCode, "Invalid refresh token.");
    }
}