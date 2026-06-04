package org.example.healthdataservice.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PbfPredictRequest {

    @JsonProperty("sex_m")
    private Integer sexM;

    private Double age;
    private Double weight;
    private Double height;
    private Double neck;
    private Double chest;
    private Double abdomen;
    private Double hip;
    private Double thigh;
}
