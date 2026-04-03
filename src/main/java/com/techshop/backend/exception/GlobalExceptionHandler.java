package com.techshop.backend.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> handleAppException(AppException ex){

        ErrorCode errorCode = ex.getErrorCode();

        Map<String,Object> response = new HashMap<>();

        response.put("code", errorCode.name());
        response.put("message", errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(response);
    }
}