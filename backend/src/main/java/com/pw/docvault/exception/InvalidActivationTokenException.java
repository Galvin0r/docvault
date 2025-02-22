package com.pw.docvault.exception;

public class InvalidActivationTokenException extends RuntimeException {
    public InvalidActivationTokenException(String message) {
        super(message);
    }
}
