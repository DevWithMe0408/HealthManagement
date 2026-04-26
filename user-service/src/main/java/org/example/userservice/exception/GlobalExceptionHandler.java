package org.example.userservice.exception;

import org.example.userservice.dto.response.DataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<DataResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode ec = ex.getErrorCode();
        log.warn("BusinessException [{}]: {}", ec.getCode(), ex.getMessage());
        return ResponseEntity.status(ec.getHttpStatus())
                .body(DataResponse.error(ec.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<DataResponse<Void>> handleAuthentication(AuthenticationException ex) {
        ErrorCode ec = ErrorCode.INVALID_CREDENTIAL;
        log.warn("AuthenticationException: {}", ex.getMessage());
        return ResponseEntity.status(ec.getHttpStatus())
                .body(DataResponse.error(ec.getCode(), ec.getDefaultMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DataResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        ErrorCode ec = ErrorCode.VALIDATION_FAILED;
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (message.isEmpty()) {
            message = ec.getDefaultMessage();
        }
        log.warn("Validation failed: {}", message);
        return ResponseEntity.status(ec.getHttpStatus())
                .body(DataResponse.error(ec.getCode(), message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DataResponse<Void>> handleUnknown(Exception ex) {
        ErrorCode ec = ErrorCode.INTERNAL_ERROR;
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(ec.getHttpStatus())
                .body(DataResponse.error(ec.getCode(), ec.getDefaultMessage()));
    }
}
