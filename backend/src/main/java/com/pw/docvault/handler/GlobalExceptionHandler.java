package com.pw.docvault.handler;

import com.pw.docvault.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ExceptionDetails(ErrorCode code, String message, int status, String path, Instant timestamp) {}

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ExceptionDetails> handleAppException(AppException ex, HttpServletRequest req) {
        var status = resolveStatus(ex);
        log.error(ex.getMessage(), ex);
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
        log.error(ex.getMessage(), ex);
        var details = new ExceptionDetails(
            ErrorCode.UNKNOWN,
            "An unexpected error occurred.",
            status.value(),
            req.getRequestURI(),
            Instant.now()
        );
        return ResponseEntity.status(status).body(details);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ExceptionDetails> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        var status = HttpStatus.NOT_FOUND;
        var details = new ExceptionDetails(
                ErrorCode.RESOURCE_NOT_FOUND,
                "Not found.",
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
