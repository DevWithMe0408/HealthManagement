package org.example.healthdataservice.dto.response;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UnitDTO {
    private Long id;

    @NotBlank(message = "Code không được để trống")
    @Size(max = 10, message = "Code không được vượt quá 10 ký tự")
    private String code;

    @NotBlank(message = "Tên đơn vị không được để trống")
    @Size(max = 100, message = "Tên đơn vị không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 255, message = "Mô tả không được vượt quá 255 ký tự")
    private String description;
}
