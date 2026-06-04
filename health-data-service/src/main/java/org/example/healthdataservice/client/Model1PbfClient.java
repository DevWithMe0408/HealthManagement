package org.example.healthdataservice.client;

import org.example.healthdataservice.dto.ml.PbfPredictRequest;
import org.example.healthdataservice.dto.ml.PbfPredictResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class Model1PbfClient {

    private static final Logger log = LoggerFactory.getLogger(Model1PbfClient.class);
    private static final String PBF_PREDICT_PATH = "/v1/predict/pbf";

    private final RestClient restClient;

    public Model1PbfClient(
            @Value("${app.ml.pbf-url}") String baseUrl,
            @Value("${app.ml.timeout-ms}") int timeoutMs
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Double predictPbf(PbfPredictRequest request) {
        PbfPredictResponse response = restClient.post()
                .uri(PBF_PREDICT_PATH)
                .body(request)
                .retrieve()
                .body(PbfPredictResponse.class);

        if (response == null || response.getPbf() == null) {
            log.warn("Model 1 PBF service returned empty response");
            return null;
        }
        log.debug("Model 1 PBF predicted value {} with model version {}", response.getPbf(), response.getModelVersion());
        return response.getPbf();
    }
}
