package org.example.healthdataservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.MeasurementDTO;
import org.example.healthdataservice.service.MeasurementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-data/measurements")
@RequiredArgsConstructor
public class MeasurementController {
    private final MeasurementService measurementService;

    @PostMapping
    public ResponseEntity<MeasurementDTO> create(@Valid @RequestBody MeasurementDTO dto) {
        return ResponseEntity.ok(measurementService.createMeasurement(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MeasurementDTO> updateByUserId(@PathVariable Long userId, @Valid @RequestBody MeasurementDTO dto) {
        return ResponseEntity.ok(measurementService.updateMeasurementByUserId(userId, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeasurementDTO> getByUserId(@PathVariable Long id) {
        return ResponseEntity.ok(measurementService.getMeasurementByUserId(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        measurementService.deleteMeasurement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<MeasurementDTO>> getAllByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(measurementService.getAllMeasurementsByUserId(userId));
    }
}
