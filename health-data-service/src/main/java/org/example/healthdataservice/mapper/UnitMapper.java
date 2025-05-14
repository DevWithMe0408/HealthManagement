package org.example.healthdataservice.mapper;

import org.example.healthdataservice.dto.response.UnitDTO;
import org.example.healthdataservice.entity.Unit;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnitMapper {
    public UnitDTO toDto(Unit unit) {
        UnitDTO dto = new UnitDTO();
        dto.setId(unit.getId());
        dto.setCode(unit.getCode());
        dto.setName(unit.getName());
        dto.setDescription(unit.getDescription());
        return dto;
    }

    public Unit toEntity(UnitDTO dto) {
        Unit unit = new Unit();
        unit.setId(dto.getId());
        unit.setCode(dto.getCode());
        unit.setName(dto.getName());
        unit.setDescription(dto.getDescription());
        return unit;
    }

    public List<Unit> toEntityList(List<UnitDTO> dtos) {
        return dtos.stream().map(this::toEntity).collect(java.util.stream.Collectors.toList());
    }

    public List<UnitDTO> toDtoList(List<Unit> entities) {
        return entities.stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

}
