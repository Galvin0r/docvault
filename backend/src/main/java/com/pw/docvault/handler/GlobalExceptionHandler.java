package com.pw.docvault.handler;

import com.pw.docvault.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ExceptionDetails(ErrorCode code, String message, int status, String path, Instant timestamp) {}

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ExceptionDetails> handleAppException(AppException ex, HttpServletRequest req) {
        var status = resolveStatus(ex);
        logger.error(ex.getMessage(), ex);
        var details = new ExceptionDetails(
                ex.getErrorCode(),
                ex.getMessage(),
                status.value(),
                req.getRequestURI(),
                Instant.now()
        );
        return ResponseEntity.status(status).body(details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionDetails> handleException(Exception ex,  HttpServletRequest req) {
        var status = resolveStatus(ex);
        logger.error(ex.getMessage(), ex);
        var details = new ExceptionDetails(
            ErrorCode.UNKNOWN,
            "An unexpected error occurred.",
            status.value(),
            req.getRequestURI(),
            Instant.now()
        );
        return ResponseEntity.status(status).body(details);
    }

    private static HttpStatus resolveStatus(Throwable ex) {
        var annotation = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        return annotation != null ? annotation.value() : HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
