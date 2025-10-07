package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends AppException {
    public NotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public NotFoundException(ErrorCode errorCode) {
        super(errorCode, "Object not found.");
    }
}
