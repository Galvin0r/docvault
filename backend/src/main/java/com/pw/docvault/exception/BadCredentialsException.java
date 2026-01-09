package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class BadCredentialsException extends AppException {
    public BadCredentialsException(ErrorCode errorCode) {
        super(errorCode, "Bad credentials.");
    }
    public BadCredentialsException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public BadCredentialsException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
