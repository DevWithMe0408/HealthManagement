package org.example.nutritionservice.entity.favorite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class FavoriteDishId implements Serializable {

    private String userId;
    private String dishId;
}
