package com.mysociety.identity.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalid(MethodArgumentNotValidException e, HttpServletRequest r) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        p.setType(URI.create("urn:mysociety:validation-error"));
        p.setProperty("errors", e.getBindingResult().getFieldErrors().stream().map(f -> Map.of("field", f.getField(), "message", f.getDefaultMessage())).toList());
        p.setInstance(URI.create(r.getRequestURI()));
        return p;
    }

    @ExceptionHandler(ResponseStatusException.class)
    ProblemDetail status(ResponseStatusException e, HttpServletRequest r) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.valueOf(e.getStatusCode().value()), e.getReason());
        p.setInstance(URI.create(r.getRequestURI()));
        return p;
    }
}
