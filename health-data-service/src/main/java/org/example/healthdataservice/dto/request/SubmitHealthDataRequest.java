package org.example.healthdataservice.dto.request;

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
    private Double BMINew;
    private Double BMRNew;
    private Double TDEENew;
    private Double PBFNew;
    private Double WHRNew;
    private Double age;
    private String gender;

}
