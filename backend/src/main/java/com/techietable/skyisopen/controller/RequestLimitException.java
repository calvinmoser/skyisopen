package com.techietable.skyisopen.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// TODO: ControllerAdvice https://www.baeldung.com/exception-handling-for-rest-with-spring
@ResponseStatus(value = HttpStatus.TOO_MANY_REQUESTS, reason = "Limit 10 requests per minute.")
public class RequestLimitException extends RuntimeException {

    public RequestLimitException(String message) {
        super(message);
    }

}
