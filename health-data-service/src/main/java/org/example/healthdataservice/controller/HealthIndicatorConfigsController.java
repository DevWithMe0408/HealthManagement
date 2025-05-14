package org.example.healthdataservice.controller;

import org.example.healthdataservice.dto.HealthIndicatorConfigsDTO;
import org.example.healthdataservice.entity.HealthIndicatorConfigs;
import org.example.healthdataservice.entity.enums.IndicatorType;
import org.example.healthdataservice.mapper.HealthIndicatorConfigsMapper;
import org.example.healthdataservice.service.HealthIndicatorConfigsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-data")
public class HealthIndicatorConfigsController {
    private final HealthIndicatorConfigsService healthIndicatorConfigsService;

    public HealthIndicatorConfigsController(HealthIndicatorConfigsService healthIndicatorConfigsService) {
        this.healthIndicatorConfigsService = healthIndicatorConfigsService;
    }
    @GetMapping("/indicator-configs")
    public ResponseEntity<List<HealthIndicatorConfigsDTO>> getAll() {
        List<HealthIndicatorConfigsDTO> list = healthIndicatorConfigsService.getAll().stream()
                .map(HealthIndicatorConfigsMapper::toDTO)
                .toList();
        return ResponseEntity.ok(list);
    }
    @GetMapping("/indicator-configs/{indicatorType}")
    public ResponseEntity<HealthIndicatorConfigs> getByIndicatorType(@PathVariable IndicatorType indicatorType) {
        return healthIndicatorConfigsService.getByType(indicatorType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @PostMapping("/indicator-configs")
    public ResponseEntity<HealthIndicatorConfigsDTO> create(@RequestBody HealthIndicatorConfigsDTO healthIndicatorConfigsDTO) {
        HealthIndicatorConfigs entity = HealthIndicatorConfigsMapper.toEntity(healthIndicatorConfigsDTO);
        HealthIndicatorConfigs saved = healthIndicatorConfigsService.save(entity);
        return ResponseEntity.status(201).body(HealthIndicatorConfigsMapper.toDTO(saved));
    }
}
