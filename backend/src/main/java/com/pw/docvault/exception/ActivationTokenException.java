package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ActivationTokenException extends AppException {
    public ActivationTokenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public ActivationTokenException(ErrorCode errorCode) {
        super(errorCode, "Invalid activation token.");
    }
}
