package com.cires.ciresbackend.service;

import com.cires.ciresbackend.dto.LocationResponseDTO;
import com.cires.ciresbackend.entity.Province;
import com.cires.ciresbackend.repository.CellRepository;
import com.cires.ciresbackend.repository.DistrictRepository;
import com.cires.ciresbackend.repository.ProvinceRepository;
import com.cires.ciresbackend.repository.SectorRepository;
import com.cires.ciresbackend.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private ProvinceRepository provinceRepo;
    @Mock
    private DistrictRepository districtRepo;
    @Mock
    private SectorRepository sectorRepo;
    @Mock
    private CellRepository cellRepo;
    @Mock
    private VillageRepository villageRepo;

    private AddressService addressService;

    @BeforeEach
    void setUp() {
        addressService = new AddressService(provinceRepo, districtRepo, sectorRepo, cellRepo, villageRepo);
    }

    @Test
    void getProvinces_returnsEmptyListWhenNotSeeded() {
        when(provinceRepo.findAll()).thenReturn(List.of());

        List<LocationResponseDTO> provinces = addressService.getProvinces();

        assertEquals(0, provinces.size());
    }

    @Test
    void getProvinces_mapsProvinceData() {
        Province province = new Province();
        province.setId(1L);
        province.setName("Kigali");
        when(provinceRepo.findAll()).thenReturn(List.of(province));

        List<LocationResponseDTO> provinces = addressService.getProvinces();

        assertEquals(1, provinces.size());
        assertEquals(1L, provinces.get(0).getId());
        assertEquals("Kigali", provinces.get(0).getName());
        assertEquals("PROVINCE", provinces.get(0).getLevelType());
    }
}

