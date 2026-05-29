package com.techietable.skyisopen.controller;

import org.springframework.http.HttpStatus;

public class SkyIsExceptional extends RuntimeException {

    private final HttpStatus status;

    public SkyIsExceptional(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public SkyIsExceptional(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
