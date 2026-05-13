package org.example.nutritionservice.entity.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PenaltyConfigId implements Serializable {
    private Integer layer;
    private Integer distanceDays;
}
