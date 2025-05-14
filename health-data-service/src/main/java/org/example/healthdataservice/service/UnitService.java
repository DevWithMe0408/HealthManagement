package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.response.UnitDTO;

import java.util.List;

public interface UnitService {
    UnitDTO create(UnitDTO dto);
    UnitDTO update(Long id, UnitDTO dto);
    void delete(Long id);
    UnitDTO getById(Long id);
    List<UnitDTO> getAll();
    List<UnitDTO> createMany(List<UnitDTO> dtos);
}
