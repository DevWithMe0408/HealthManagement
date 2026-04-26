package org.example.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
