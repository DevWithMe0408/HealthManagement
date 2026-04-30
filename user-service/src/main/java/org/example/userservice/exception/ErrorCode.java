package org.example.userservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // ===== AUTH module =====
    USERNAME_TAKEN          ("AUTH-001", HttpStatus.CONFLICT,                "Username đã tồn tại"),
    EMAIL_TAKEN             ("AUTH-002", HttpStatus.CONFLICT,                "Email đã tồn tại"),
    INVALID_CREDENTIAL      ("AUTH-003", HttpStatus.UNAUTHORIZED,            "Sai username hoặc password"),
    REFRESH_TOKEN_NOT_FOUND ("AUTH-004", HttpStatus.UNAUTHORIZED,            "Refresh token không hợp lệ"),
    REFRESH_TOKEN_EXPIRED   ("AUTH-005", HttpStatus.UNAUTHORIZED,            "Refresh token đã hết hạn"),
    JWT_INVALID             ("AUTH-006", HttpStatus.UNAUTHORIZED,            "JWT không hợp lệ"),
    JWT_EXPIRED             ("AUTH-007", HttpStatus.UNAUTHORIZED,            "JWT đã hết hạn"),
    EVENT_PUBLISH_FAILED    ("AUTH-500", HttpStatus.INTERNAL_SERVER_ERROR,   "Không thể publish event"),

    // ===== USER module =====
    USER_NOT_FOUND          ("USER-001", HttpStatus.NOT_FOUND,               "User không tồn tại"),

    // ===== Common =====
    VALIDATION_FAILED       ("COMMON-001", HttpStatus.BAD_REQUEST,           "Dữ liệu không hợp lệ"),
    INTERNAL_ERROR          ("COMMON-500", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
