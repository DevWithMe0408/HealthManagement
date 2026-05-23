package org.example.nutritionservice.exception;

import lombok.Getter;

@Getter
public class RecommendationTooComplexException extends RuntimeException {

    private final long estimatedCombinations;

    public RecommendationTooComplexException(long estimatedCombinations) {
        super("Cau hinh bua qua phuc tap, vui long giam so mon/slot. Uoc tinh "
                + estimatedCombinations + " to hop.");
        this.estimatedCombinations = estimatedCombinations;
    }
}
