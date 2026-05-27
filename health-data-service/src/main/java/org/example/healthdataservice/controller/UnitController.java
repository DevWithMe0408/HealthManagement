package org.example.healthdataservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.response.UnitDTO;
import org.example.healthdataservice.service.UnitService;
import org.example.web.dto.response.DataResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/health-data/units")
@RequiredArgsConstructor
public class UnitController {
    private final UnitService service;

    @PostMapping
    public ResponseEntity<DataResponse<UnitDTO>> create(@Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(DataResponse.success(service.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataResponse<UnitDTO>> update(@PathVariable Long id, @Valid @RequestBody UnitDTO dto) {
        return ResponseEntity.ok(DataResponse.success(service.update(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(DataResponse.success());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataResponse<UnitDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(DataResponse.success(service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<DataResponse<List<UnitDTO>>> getAll() {
        return ResponseEntity.ok(DataResponse.success(service.getAll()));
    }
    @PostMapping("/batch")
    public ResponseEntity<DataResponse<List<UnitDTO>>> createMany(@Valid @RequestBody List<@Valid UnitDTO> dtos) {
        return ResponseEntity.ok(DataResponse.success(service.createMany(dtos)));
    }

}
