package com.techietable.skyisopen.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SkyIsExceptional.class)
    public ResponseEntity<String> handleSkyIsExceptional(SkyIsExceptional e) {
        log.error("AeroAPI error: status={}, message={}", e.getStatus(), e.getMessage(), e);
        return ResponseEntity.status(e.getStatus()).body(e.getMessage());
    }

    @ExceptionHandler(RequestLimitException.class)
    public ResponseEntity<String> handleRequestLimitException(RequestLimitException e) {
        log.warn("Rate limit exceeded: {}", e.getMessage());
        return ResponseEntity.status(429).body(e.getMessage());
    }
}
