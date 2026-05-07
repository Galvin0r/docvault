package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends AppException {
    public ForbiddenException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public ForbiddenException(ErrorCode errorCode) {
        super(errorCode, "Operation not permitted.");
    }
}