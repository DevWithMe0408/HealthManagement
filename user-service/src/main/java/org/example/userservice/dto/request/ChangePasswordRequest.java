package org.example.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Mat khau hien tai khong duoc bo trong")
    private String currentPassword;

    @NotBlank(message = "Mat khau moi khong duoc bo trong")
    @Size(min = 8, max = 100, message = "Mat khau moi phai tu 8 den 100 ky tu")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*\\d).+$",
            message = "Mat khau moi phai co it nhat 1 chu HOA va 1 chu so"
    )
    private String newPassword;
}
