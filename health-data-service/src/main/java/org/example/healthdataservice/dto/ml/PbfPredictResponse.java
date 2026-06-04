package org.example.healthdataservice.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PbfPredictResponse {

    private Double pbf;

    @JsonProperty("model_version")
    private String modelVersion;
}
