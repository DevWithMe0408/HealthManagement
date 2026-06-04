package org.example.healthdataservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConstitutionResponse {
    private String constitution;
    private String method;
    private Double bmi;
    private Double pbf;
    private Double pbfFormula;
    private Double pbfModel;
    private String pbfSource;
    private Integer bmiClass;
    private Integer pbfClass;
    private Integer finalClass;
    private String suggestedGoal;
    private String warning;
    private LocalDateTime computedAt;
}
