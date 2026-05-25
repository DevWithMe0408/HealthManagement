package org.example.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PreferenceUpdateRequest {

    @NotBlank
    private String prefValue;

    private String valueType;
}
