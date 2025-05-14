package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.MeasurementDTO;
import org.example.healthdataservice.entity.Measurement;
import org.example.healthdataservice.mapper.MeasurementMapper;
import org.example.healthdataservice.repository.MeasurementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeasurementServiceImpl implements MeasurementService {
    private final MeasurementRepository measurementRepository;
    private final MeasurementMapper measurementMapper;

    @Override
    public MeasurementDTO createMeasurement(MeasurementDTO dto) {
        Measurement measurement = measurementMapper.toEntity(dto);
        measurement.setUpdatedAt(java.time.LocalDateTime.now());
        return measurementMapper.toDto(measurementRepository.save(measurement));
    }

    @Override
    public MeasurementDTO updateMeasurementByUserId(Long userId, MeasurementDTO dto) {
        Measurement existing = measurementRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bản ghi Measurement với userID: " + userId));
        existing.setMeasurement_time(dto.getMeasurementTime());
        existing.setYear(dto.getYear());
        existing.setMonth(dto.getMonth());
        existing.setWeek_number(dto.getWeekNumber());
        existing.setUpdatedAt(java.time.LocalDateTime.now());
        return measurementMapper.toDto(measurementRepository.save(existing));
    }

    @Override
    public MeasurementDTO getMeasurementByUserId(Long userId) {
        return measurementRepository.findByUserId(userId)
                .map(measurementMapper::toDto)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Measurement với userID: " + userId));
    }

    @Override
    public void deleteMeasurement(Long id) {
        measurementRepository.deleteById(id);
    }

    @Override
    public List<MeasurementDTO> getAllMeasurementsByUserId(Long userId) {
        return measurementRepository.findAllByUserId(userId).stream()
                .map(measurementMapper::toDto)
                .collect(Collectors.toList());
    }
}
