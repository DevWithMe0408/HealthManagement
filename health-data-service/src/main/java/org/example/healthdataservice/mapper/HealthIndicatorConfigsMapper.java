package org.example.healthdataservice.mapper;

import org.example.healthdataservice.dto.HealthIndicatorConfigsDTO;
import org.example.healthdataservice.entity.HealthIndicatorConfigs;

public class HealthIndicatorConfigsMapper {

    public static HealthIndicatorConfigsDTO toDTO(HealthIndicatorConfigs entity) {
        HealthIndicatorConfigsDTO dto = new HealthIndicatorConfigsDTO();
        dto.setId(entity.getId());
        dto.setIndicatorType(entity.getIndicatorType());
        dto.setDisplayName(entity.getDisplayName());
        //dto.setUnit(entity.getUnit());
        dto.setMeasurementFrequency(entity.getMeasurementFrequency());
        dto.setActive(entity.isActive());
        return dto;
    }

    public static HealthIndicatorConfigs toEntity(HealthIndicatorConfigsDTO dto) {
        HealthIndicatorConfigs entity = new HealthIndicatorConfigs();
        entity.setId(dto.getId());
        entity.setIndicatorType(dto.getIndicatorType());
        entity.setDisplayName(dto.getDisplayName());
        //entity.setUnit(dto.getUnit());
        entity.setMeasurementFrequency(dto.getMeasurementFrequency());
        entity.setActive(dto.isActive());
        return entity;
    }
}
