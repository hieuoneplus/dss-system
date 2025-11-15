package com.example.nsga;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ExceptionControllerHandler {
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleNotReadable(IllegalStateException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), req);
    }

    private ResponseEntity<ApiError> build(HttpStatus status,
                                           String code,
                                           String message,
                                           HttpServletRequest req
                                           ) {
        ApiError body = new ApiError();
        body.setStatus(status.value());
        body.setError(status.getReasonPhrase());
        body.setCode(code);
        body.setMessage(message);
        body.setPath(req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
