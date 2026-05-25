package org.example.healthdataservice.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCalculatorTest {

    private final HealthCalculator healthCalculator = new HealthCalculator();

    @Test
    void calculatePbfDoesNotRequireAgeForMale() {
        Double pbf = healthCalculator.calculatePBF("MALE", 84.0, null, 38.0, 175.0, null);

        assertThat(pbf).isNotNull();
        assertThat(pbf).isPositive();
    }

    @Test
    void calculatePbfDoesNotRequireAgeForFemale() {
        Double pbf = healthCalculator.calculatePBF("FEMALE", 72.0, 95.0, 34.0, 165.0, null);

        assertThat(pbf).isNotNull();
        assertThat(pbf).isPositive();
    }

    @Test
    void calculatePbfStillRequiresFemaleHipMeasurement() {
        Double pbf = healthCalculator.calculatePBF("FEMALE", 72.0, null, 34.0, 165.0, null);

        assertThat(pbf).isNull();
    }
}
