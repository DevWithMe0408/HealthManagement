package org.example.web.exception;

import lombok.Getter;

/**
 * Exception cho business logic. Khi throw exception nay,
 * GlobalExceptionHandler se bat va tra response chuan.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String customMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.customMessage = customMessage;
    }

    public String getEffectiveMessage() {
        return customMessage != null ? customMessage : errorCode.getDefaultMessage();
    }
}