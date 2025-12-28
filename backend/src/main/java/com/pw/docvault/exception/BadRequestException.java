package com.pw.docvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException  extends AppException{
    public BadRequestException(ErrorCode errorCode, String message) { super(errorCode, message); }
    public BadRequestException(ErrorCode errorCode) { super(errorCode, "Bad request."); }
}
