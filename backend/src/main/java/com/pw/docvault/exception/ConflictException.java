package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends AppException {
    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}