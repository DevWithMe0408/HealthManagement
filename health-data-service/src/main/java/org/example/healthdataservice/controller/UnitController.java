package org.example.healthdataservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.response.UnitDTO;
import org.example.healthdataservice.service.UnitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-data/units")
@RequiredArgsConstructor
public class UnitController {
    private final UnitService service;

    @PostMapping
    public ResponseEntity<UnitDTO> create(@Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UnitDTO> update(@PathVariable Long id, @Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UnitDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<UnitDTO>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
    @PostMapping("/batch")
    public ResponseEntity<List<UnitDTO>> createMany(@Valid @RequestBody List<@Valid UnitDTO> dtos) {
        return ResponseEntity.ok(service.createMany(dtos));
    }

}
