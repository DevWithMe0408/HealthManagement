package org.example.userservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferenceResponse {
    private String prefKey;
    private String prefValue;
    private String valueType;
    private String description;
}
