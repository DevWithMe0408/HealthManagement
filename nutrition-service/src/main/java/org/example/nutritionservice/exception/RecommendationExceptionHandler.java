package org.example.nutritionservice.exception;

import org.example.web.dto.response.DataResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RecommendationExceptionHandler {

    @ExceptionHandler(RecommendationTooComplexException.class)
    public ResponseEntity<DataResponse<Void>> handleTooComplex(RecommendationTooComplexException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(DataResponse.error("RECOMMENDATION-422", ex.getMessage()));
    }
}
