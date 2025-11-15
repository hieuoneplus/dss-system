package com.example.nsga;

import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex,
                                                      HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request body không hợp lệ", req, null);
    }
}
