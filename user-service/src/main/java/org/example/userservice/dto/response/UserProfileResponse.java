package org.example.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private Long userId; // Thêm userId
    private String username;
    private List<String> roles;
    // Thêm các trường khác nếu muốn trả về (ví dụ: email, name từ bảng User)
}