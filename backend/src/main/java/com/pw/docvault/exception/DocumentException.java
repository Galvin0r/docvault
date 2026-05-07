package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class DocumentException extends AppException {
    public DocumentException(ErrorCode errorCode, String message) { super(errorCode, message); }
    public DocumentException(ErrorCode errorCode, String message, Throwable cause) { super(errorCode, message, cause); }
}