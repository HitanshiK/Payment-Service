package com.paymentSystem.project.exceptions;

import com.paymentSystem.project.dto.response.ApiError;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex) {
        ApiError error = new ApiError(
                ex.getErrorCode(),
                ex.getMessage(),
                ex.getHttpStatus()
        );
        return new ResponseEntity<>(error, HttpStatus.valueOf(ex.getHttpStatus()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {
        ApiError error = new ApiError(
                "VALIDATION_ERROR",
                ex.getBindingResult().getFieldError().getDefaultMessage(),
                400
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUserRegisteredException() {
        ApiError error = new ApiError(
                "USER_ALREADY_PRESENT",
                "User Already Present with this email or phone",
                400
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        // Log full error internally
        ex.printStackTrace();

        ApiError error = new ApiError(
                "INTERNAL_SERVER_ERROR",
                "Something went wrong. Please try again later.",
                500
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
