package org.example.web.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response wrapper chuan cho moi REST endpoint.
 * - Success: code = null, message = "Success", data = payload
 * - Error:   code = error code (vd "AUTH-001"), message = mo ta loi, data = null
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DataResponse<T> {
    private static final String SUCCESS_MESSAGE = "Success";

    private String code;
    private String message;
    private T data;

    public static <T> DataResponse<T> success(T data) {
        return new DataResponse<>(null, SUCCESS_MESSAGE, data);
    }

    public static <T> DataResponse<T> success() {
        return new DataResponse<>(null, SUCCESS_MESSAGE, null);
    }

    public static <T> DataResponse<T> error(String code, String message) {
        return new DataResponse<>(code, message, null);
    }
}
