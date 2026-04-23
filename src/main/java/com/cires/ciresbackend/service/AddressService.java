package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.LocationResponseDTO;
import com.cires.ciresbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final ProvinceRepository provinceRepo;
    private final DistrictRepository districtRepo;
    private final SectorRepository sectorRepo;
    private final CellRepository cellRepo;
    private final VillageRepository villageRepo;

    public List<LocationResponseDTO> getProvinces() {
        return provinceRepo.findAll().stream()
                .map(p -> new LocationResponseDTO(p.getId(), p.getName(), "PROVINCE"))
                .collect(Collectors.toList());
    }

    public List<LocationResponseDTO> getDistrictsByProvince(Long id) {
        if (!provinceRepo.existsById(id)) {
            throw new IllegalArgumentException("Province not found");
        }
        return districtRepo.findByProvinceId(id).stream()
                .map(d -> new LocationResponseDTO(d.getId(), d.getName(), "DISTRICT"))
                .collect(Collectors.toList());
    }

    public List<LocationResponseDTO> getSectorsByDistrict(Long id) {
        if (!districtRepo.existsById(id)) {
            throw new IllegalArgumentException("District not found");
        }
        return sectorRepo.findByDistrictId(id).stream()
                .map(s -> new LocationResponseDTO(s.getId(), s.getName(), "SECTOR"))
                .collect(Collectors.toList());
    }

    public List<LocationResponseDTO> getCellsBySector(Long id) {
        if (!sectorRepo.existsById(id)) {
            throw new IllegalArgumentException("Sector not found");
        }
        return cellRepo.findBySectorId(id).stream()
                .map(c -> new LocationResponseDTO(c.getId(), c.getName(), "CELL"))
                .collect(Collectors.toList());
    }

    public List<LocationResponseDTO> getVillagesByCell(Long id) {
        if (!cellRepo.existsById(id)) {
            throw new IllegalArgumentException("Cell not found");
        }
        return villageRepo.findByCellId(id).stream()
                .map(v -> new LocationResponseDTO(v.getId(), v.getName(), "VILLAGE"))
                .collect(Collectors.toList());
    }
}