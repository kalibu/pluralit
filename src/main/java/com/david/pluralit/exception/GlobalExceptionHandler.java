package com.david.pluralit.exception;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidProcessStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProcessState(InvalidProcessStateException ex) {
        ErrorResponse response = new ErrorResponse();
        response.setProcessId(ex.getProcessId());
        response.setStatus(ex.getCurrentStatus().name());
        response.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class ErrorResponse {
        private UUID processId;
        private String status;
        private String message;
    }
}
