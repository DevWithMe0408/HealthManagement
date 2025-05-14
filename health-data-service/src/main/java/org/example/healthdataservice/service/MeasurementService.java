package org.example.healthdataservice.service;

import org.example.healthdataservice.dto.MeasurementDTO;

import java.util.List;

public interface MeasurementService {
    MeasurementDTO createMeasurement(MeasurementDTO dto);
    MeasurementDTO updateMeasurementByUserId(Long userId, MeasurementDTO dto);
    MeasurementDTO getMeasurementByUserId(Long id);
    void deleteMeasurement(Long id);
    List<MeasurementDTO> getAllMeasurementsByUserId(Long userId);
}
