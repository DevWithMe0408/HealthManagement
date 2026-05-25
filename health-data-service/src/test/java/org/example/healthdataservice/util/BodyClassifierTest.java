package org.example.healthdataservice.util;

import org.example.healthdataservice.entity.enums.Gender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyClassifierTest {

    private final BodyClassifier bodyClassifier = new BodyClassifier();

    @Test
    void classifyByBmiUsesAsianCutoffs() {
        assertThat(bodyClassifier.classifyByBmi(18.49)).isEqualTo(0);
        assertThat(bodyClassifier.classifyByBmi(18.5)).isEqualTo(1);
        assertThat(bodyClassifier.classifyByBmi(22.99)).isEqualTo(1);
        assertThat(bodyClassifier.classifyByBmi(23.0)).isEqualTo(2);
        assertThat(bodyClassifier.classifyByBmi(24.99)).isEqualTo(2);
        assertThat(bodyClassifier.classifyByBmi(25.0)).isEqualTo(3);
    }

    @Test
    void classifyByPbfUsesGenderSpecificCutoffs() {
        assertThat(bodyClassifier.classifyByPbf(7.99, Gender.MALE)).isEqualTo(0);
        assertThat(bodyClassifier.classifyByPbf(8.0, Gender.MALE)).isEqualTo(1);
        assertThat(bodyClassifier.classifyByPbf(25.0, Gender.MALE)).isEqualTo(2);
        assertThat(bodyClassifier.classifyByPbf(30.0, Gender.MALE)).isEqualTo(3);

        assertThat(bodyClassifier.classifyByPbf(20.99, Gender.FEMALE)).isEqualTo(0);
        assertThat(bodyClassifier.classifyByPbf(21.0, Gender.FEMALE)).isEqualTo(1);
        assertThat(bodyClassifier.classifyByPbf(36.0, Gender.FEMALE)).isEqualTo(2);
        assertThat(bodyClassifier.classifyByPbf(42.0, Gender.FEMALE)).isEqualTo(3);
    }

    @Test
    void classifyFinalKeepsWorstClassAndFallsBackToBmi() {
        assertThat(bodyClassifier.classifyFinal(1, 2)).isEqualTo(2);
        assertThat(bodyClassifier.classifyFinal(3, 1)).isEqualTo(3);
        assertThat(bodyClassifier.classifyFinal(1, null)).isEqualTo(1);
        assertThat(bodyClassifier.classifyFinal(null, 1)).isNull();
    }

    @Test
    void classNameMapsExpectedGoalClasses() {
        assertThat(bodyClassifier.classNameOf(0)).isEqualTo("GAY");
        assertThat(bodyClassifier.classNameOf(1)).isEqualTo("CAN_DOI");
        assertThat(bodyClassifier.classNameOf(2)).isEqualTo("THUA_CAN");
        assertThat(bodyClassifier.classNameOf(3)).isEqualTo("BEO_PHI");
        assertThat(bodyClassifier.classNameOf(4)).isNull();
    }
}
