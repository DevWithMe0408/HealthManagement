package org.example.healthdataservice.service;

import lombok.RequiredArgsConstructor;
import org.example.healthdataservice.dto.response.UnitDTO;
import org.example.healthdataservice.entity.Unit;
import org.example.healthdataservice.mapper.UnitMapper;
import org.example.healthdataservice.repository.UnitRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {
    private final UnitRepository unitRepository;
    private final UnitMapper mapper;

    @Override
    public UnitDTO create(UnitDTO dto) {
        if (unitRepository.existsByCode(dto.getCode())) {
            throw new IllegalArgumentException("Code đã tồn tại");
        }
        Unit unit = mapper.toEntity(dto);
        return mapper.toDto(unitRepository.save(unit));
    }

    @Override
    public UnitDTO update(Long id, UnitDTO dto) {
        Unit existing = unitRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị"));

        existing.setCode(dto.getCode());
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());

        return mapper.toDto(unitRepository.save(existing));
    }

    @Override
    public void delete(Long id) {
        unitRepository.deleteById(id);
    }

    @Override
    public UnitDTO getById(Long id) {
        return unitRepository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn vị"));
    }

    @Override
    public List<UnitDTO> getAll() {
        return unitRepository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }
    @Override
    public List<UnitDTO> createMany(List<UnitDTO> dtos) {
        // Kiểm tra trùng code
        for (UnitDTO dto : dtos) {
            if (unitRepository.existsByCode(dto.getCode())) {
                throw new IllegalArgumentException("Code đã tồn tại: " + dto.getCode());
            }
        }

        List<Unit> entities = mapper.toEntityList(dtos);
        List<Unit> saved = unitRepository.saveAll(entities);
        return mapper.toDtoList(saved);
    }

}
