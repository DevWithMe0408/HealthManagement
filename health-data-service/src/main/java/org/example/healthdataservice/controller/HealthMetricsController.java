package org.example.healthdataservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.HealthMetricsDTO;
import org.example.healthdataservice.service.HealthMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-data/metrics")
@RequiredArgsConstructor
public class HealthMetricsController {

    private final HealthMetricsService service;

    @PostMapping
    public ResponseEntity<HealthMetricsDTO> create(@Valid @RequestBody HealthMetricsDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HealthMetricsDTO> update(@Valid @PathVariable Long id, @RequestBody HealthMetricsDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<HealthMetricsDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<HealthMetricsDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
}
