package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PasswordResetTokenException extends AppException {
    public PasswordResetTokenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public PasswordResetTokenException(ErrorCode errorCode) {
        super(errorCode, "Invalid password reset token.");
    }
}
