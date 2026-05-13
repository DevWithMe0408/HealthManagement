package org.example.web.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Error code chung cho toan he thong.
 * Format: <module>-<num>
 * Khi them error code moi, them vao day theo nhom logic.
 */
@Getter
public enum ErrorCode {
    // ===== AUTH module =====
    USER_NOT_FOUND          ("AUTH-001", HttpStatus.NOT_FOUND,    "Nguoi dung khong ton tai"),
    INVALID_CREDENTIALS     ("AUTH-002", HttpStatus.UNAUTHORIZED, "Sai username hoac password"),
    USERNAME_TAKEN          ("AUTH-003", HttpStatus.CONFLICT,     "Username da ton tai"),
    EMAIL_TAKEN             ("AUTH-004", HttpStatus.CONFLICT,     "Email da ton tai"),
    REFRESH_TOKEN_INVALID   ("AUTH-005", HttpStatus.UNAUTHORIZED, "Refresh token khong hop le"),
    REFRESH_TOKEN_EXPIRED   ("AUTH-006", HttpStatus.UNAUTHORIZED, "Refresh token het han"),
    UNAUTHORIZED            ("AUTH-007", HttpStatus.UNAUTHORIZED, "Khong co quyen truy cap"),
    FORBIDDEN               ("AUTH-008", HttpStatus.FORBIDDEN,    "Khong du quyen thuc hien thao tac"),
    JWT_INVALID             ("AUTH-009", HttpStatus.UNAUTHORIZED, "JWT khong hop le"),
    JWT_EXPIRED             ("AUTH-010", HttpStatus.UNAUTHORIZED, "JWT da het han"),
    EVENT_PUBLISH_FAILED    ("AUTH-500", HttpStatus.INTERNAL_SERVER_ERROR, "Khong the publish event"),

    // ===== CONFIG module =====
    CONFIG_NOT_FOUND        ("CONFIG-001", HttpStatus.NOT_FOUND,   "Cau hinh khong ton tai"),
    CONFIG_VALIDATION_FAILED("CONFIG-002", HttpStatus.BAD_REQUEST, "Cau hinh khong hop le"),
    CONFIG_SUM_INVALID      ("CONFIG-003", HttpStatus.BAD_REQUEST, "Tong gia tri phai bang gia tri mong doi"),

    // ===== COMMON =====
    VALIDATION_FAILED       ("COMMON-001", HttpStatus.BAD_REQUEST,           "Du lieu khong hop le"),
    INTERNAL_SERVER_ERROR   ("COMMON-002", HttpStatus.INTERNAL_SERVER_ERROR, "Loi he thong");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
