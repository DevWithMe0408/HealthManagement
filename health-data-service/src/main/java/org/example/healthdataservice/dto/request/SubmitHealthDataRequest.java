package org.example.healthdataservice.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitHealthDataRequest {
    private Long userId;
    private Double height;
    private Double weight;
    private Double waist;
    private Double hip;
    private Double neck;
    private Double bust;
    private Double activityFactor;

    @JsonProperty("BMINew")
    private Double BMINew;

    @JsonProperty("BMRNew")
    private Double BMRNew;

    @JsonProperty("TDEENew")
    private Double TDEENew;

    @JsonProperty("PBFNew")
    private Double PBFNew;

    @JsonProperty("WHRNew")
    private Double WHRNew;

    private Double age;
    private String gender;
}
