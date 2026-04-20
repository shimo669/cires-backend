package com.cires.ciresbackend.exception;

import com.cires.ciresbackend.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(404, ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<ApiResponse<?>> handleForbidden(ForbiddenActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiResponse<>(403, ex.getMessage()));
    }

    @ExceptionHandler({InvalidRequestException.class, IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<?>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(400, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiResponse<>(500, "Unexpected server error"));
    }
}

