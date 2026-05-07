package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class AlreadyExistsException extends AppException {
    public AlreadyExistsException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    public AlreadyExistsException(ErrorCode errorCode) {
        super(errorCode, "Object already exists.");
    }
}