package com.cires.ciresbackend.controller;

import com.cires.ciresbackend.dto.LocationResponseDTO;
import com.cires.ciresbackend.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
// Updated to include /api prefix to match Frontend and SecurityConfig
@RequestMapping({"/api/address", "/api/locations"})
@RequiredArgsConstructor
// Specific origins are safer than "*" when using allowCredentials in SecurityConfig
@CrossOrigin(origins = {"https://cires-frontend.onrender.com", "http://localhost:5173"}, allowCredentials = "true")
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/provinces")
    public ResponseEntity<List<LocationResponseDTO>> getProvinces() {
        try {
            List<LocationResponseDTO> provinces = addressService.getProvinces();
            return ResponseEntity.ok(provinces);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/provinces/{provinceId}/districts")
    public ResponseEntity<List<LocationResponseDTO>> getDistrictsByProvince(@PathVariable Long provinceId) {
        try {
            List<LocationResponseDTO> districts = addressService.getDistrictsByProvince(provinceId);
            return ResponseEntity.ok(districts);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/districts/{districtId}/sectors")
    public ResponseEntity<List<LocationResponseDTO>> getSectorsByDistrict(@PathVariable Long districtId) {
        try {
            List<LocationResponseDTO> sectors = addressService.getSectorsByDistrict(districtId);
            return ResponseEntity.ok(sectors);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/sectors/{sectorId}/cells")
    public ResponseEntity<List<LocationResponseDTO>> getCellsBySector(@PathVariable Long sectorId) {
        try {
            List<LocationResponseDTO> cells = addressService.getCellsBySector(sectorId);
            return ResponseEntity.ok(cells);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/cells/{cellId}/villages")
    public ResponseEntity<List<LocationResponseDTO>> getVillagesByCell(@PathVariable Long cellId) {
        try {
            List<LocationResponseDTO> villages = addressService.getVillagesByCell(cellId);
            return ResponseEntity.ok(villages);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}