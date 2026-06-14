package com.sharedexpenses;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public AppException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public static AppException notFound(String msg) {
        return new AppException(HttpStatus.NOT_FOUND, msg);
    }

    public HttpStatus getStatus() {
        return status;
    }
}
