package com.auction.auctionservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> f = new LinkedHashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) f.put(e.getField(), e.getDefaultMessage());
        return ResponseEntity.badRequest().body(ApiError.builder().timestamp(Instant.now()).status(400).error("Bad Request").message("Validation failed").path(req.getRequestURI()).fieldErrors(f).build());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> bad(BadRequestException ex, HttpServletRequest r) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), r);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> nf(NotFoundException ex, HttpServletRequest r) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), r);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> forb(ForbiddenException ex, HttpServletRequest r) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), r);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> gen(Exception ex, HttpServletRequest r) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", r);
    }

    private ResponseEntity<ApiError> build(HttpStatus s, String m, HttpServletRequest r) {
        return ResponseEntity.status(s).body(ApiError.builder().timestamp(Instant.now()).status(s.value()).error(s.getReasonPhrase()).message(m).path(r.getRequestURI()).build());
    }
}
