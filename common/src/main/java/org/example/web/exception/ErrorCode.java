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
    CHANGE_PASSWORD_WRONG_CURRENT ("AUTH-011", HttpStatus.BAD_REQUEST, "Mat khau hien tai khong dung"),
    CHANGE_PASSWORD_SAME          ("AUTH-012", HttpStatus.BAD_REQUEST, "Mat khau moi khong duoc trung mat khau cu"),
    EVENT_PUBLISH_FAILED    ("AUTH-500", HttpStatus.INTERNAL_SERVER_ERROR, "Khong the publish event"),

    // ===== CONFIG module =====
    CONFIG_NOT_FOUND        ("CONFIG-001", HttpStatus.NOT_FOUND,   "Cau hinh khong ton tai"),
    CONFIG_VALIDATION_FAILED("CONFIG-002", HttpStatus.BAD_REQUEST, "Cau hinh khong hop le"),
    CONFIG_SUM_INVALID      ("CONFIG-003", HttpStatus.BAD_REQUEST, "Tong gia tri phai bang gia tri mong doi"),

    // ===== CATALOG module =====
    DISH_NOT_FOUND          ("CATALOG-001", HttpStatus.NOT_FOUND,   "Mon an khong ton tai"),
    DISH_NAME_TAKEN         ("CATALOG-002", HttpStatus.CONFLICT,    "Ten mon da ton tai"),
    MEAL_LOG_NOT_FOUND      ("MEALLOG-001", HttpStatus.NOT_FOUND,   "Khong tim thay ban ghi bua an"),

    // ===== USER SETTINGS / GOALS =====
    PREFERENCE_INVALID_KEY  ("PREF-001", HttpStatus.BAD_REQUEST, "Key preference khong duoc ho tro"),
    PREFERENCE_INVALID_VALUE("PREF-002", HttpStatus.BAD_REQUEST, "Gia tri preference khong hop le"),
    PREFERENCE_NOT_FOUND    ("PREF-003", HttpStatus.NOT_FOUND,   "Khong tim thay preference"),
    GOAL_NO_ACTIVE          ("GOAL-001", HttpStatus.NOT_FOUND,   "Chua co muc tieu dang active"),
    GOAL_INVALID            ("GOAL-002", HttpStatus.BAD_REQUEST, "Muc tieu khong hop le"),

    // ===== HEALTH DATA / DASHBOARD =====
    HEALTH_MISSING_BASIC_DATA("HEALTH-001", HttpStatus.UNPROCESSABLE_ENTITY, "Can chieu cao va can nang de xac dinh the trang"),
    HEALTH_MISSING_GENDER   ("HEALTH-002", HttpStatus.UNPROCESSABLE_ENTITY, "Can gioi tinh de xac dinh the trang"),
    HEALTH_INVALID_METRIC   ("HEALTH-003", HttpStatus.BAD_REQUEST, "Chỉ số sức khỏe không hợp lệ"),

    // ===== COMMON =====
    VALIDATION_FAILED       ("COMMON-001", HttpStatus.BAD_REQUEST,           "Dữ liệu không hợp lệ"),
    INTERNAL_SERVER_ERROR   ("COMMON-002", HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");

    private final String code;
    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus httpStatus, String defaultMessage) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }
}
