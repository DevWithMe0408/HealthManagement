package org.example.web.exception;

import org.example.web.dto.response.DataResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
        ErrorCode code = ex.getErrorCode();
        log.warn("BusinessException [{}]: {}", code.getCode(), ex.getEffectiveMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(DataResponse.error(code.getCode(), ex.getEffectiveMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<DataResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", details);
        return ResponseEntity
                .status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(DataResponse.error(
                        ErrorCode.VALIDATION_FAILED.getCode(),
                        "Du lieu khong hop le: " + details
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<DataResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.FORBIDDEN.getHttpStatus())
                .body(DataResponse.error(
                        ErrorCode.FORBIDDEN.getCode(),
                        ErrorCode.FORBIDDEN.getDefaultMessage()
                ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<DataResponse<Void>> handleAuth(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.UNAUTHORIZED.getHttpStatus())
                .body(DataResponse.error(
                        ErrorCode.UNAUTHORIZED.getCode(),
                        ErrorCode.UNAUTHORIZED.getDefaultMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<DataResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(DataResponse.error(
                        ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
                ));
    }
}
