package org.example.healthdataservice.mapper;

import org.example.healthdataservice.dto.MeasurementDTO;
import org.example.healthdataservice.entity.Measurement;
import org.springframework.stereotype.Component;

@Component
public class MeasurementMapper {

    public MeasurementDTO toDto(Measurement entity) {
        MeasurementDTO dto = new MeasurementDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setMeasurementTime(entity.getMeasurement_time());
        dto.setYear(entity.getYear());
        dto.setMonth(entity.getMonth());
        dto.setWeekNumber(entity.getWeek_number());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public Measurement toEntity(MeasurementDTO dto) {
        Measurement entity = new Measurement();
        entity.setId(dto.getId());
        entity.setUserId(dto.getUserId());
        entity.setMeasurement_time(dto.getMeasurementTime());
        entity.setYear(dto.getYear());
        entity.setMonth(dto.getMonth());
        entity.setWeek_number(dto.getWeekNumber());
        entity.setUpdatedAt(dto.getUpdatedAt());
        return entity;
    }
}
