package com.nhnacademy.cruxmate.common.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception
    ) {
        ErrorCode errorCode = exception.getErrorCode();

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(exception.getErrorCode().getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ){
        Map<String, String> errors = new LinkedHashMap<>();

        for(FieldError fieldError : exception.getBindingResult().getFieldErrors()){
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage(), errors);

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception){
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;

        ErrorResponse response = ErrorResponse.of(errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}