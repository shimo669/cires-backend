package com.cires.ciresbackend.config;

import com.cires.ciresbackend.entity.*;
import com.cires.ciresbackend.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(
            ProvinceRepository provinceRepo,
            DistrictRepository districtRepo,
            SectorRepository sectorRepo,
            CellRepository cellRepo,
            VillageRepository villageRepo,
            CategoryRepository categoryRepo) {
        return args -> {

            // --- PHASE 1: SEED CATEGORIES ---
            if (categoryRepo.count() == 0) {
                System.out.println("Seeding default categories...");
                List<String> defaultCategories = List.of("Water", "Electricity", "Roads", "Security", "Health", "Other");

                for (String name : defaultCategories) {
                    Category category = new Category();
                    category.setCategoryName(name);
                    category.setDescription("Issues related to " + name);
                    categoryRepo.save(category);
                }
                System.out.println("Default categories successfully seeded!");
            }

            // --- PHASE 2: SEED GEOGRAPHY ---
            System.out.println("Seeding geographic data from locations.json...");
            ObjectMapper mapper = new ObjectMapper();

            TypeReference<List<Map<String, Object>>> typeReference = new TypeReference<>() {};
            try (InputStream inputStream = DataSeeder.class.getResourceAsStream("/locations.json")) {
                if (inputStream == null) {
                    System.out.println("Error: locations.json not found in resources folder.");
                    return;
                }

                List<Map<String, Object>> locations = mapper.readValue(inputStream, typeReference);

                Map<String, Province> provinceMap = provinceRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                Province::getName,
                                province -> province,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, District> districtMap = districtRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                district -> key(district.getProvince().getName(), district.getName()),
                                district -> district,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, Sector> sectorMap = sectorRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                sector -> key(sector.getDistrict().getProvince().getName(), sector.getDistrict().getName(), sector.getName()),
                                sector -> sector,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, Cell> cellMap = cellRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                cell -> key(cell.getSector().getDistrict().getProvince().getName(), cell.getSector().getDistrict().getName(), cell.getSector().getName(), cell.getName()),
                                cell -> cell,
                                (existing, duplicate) -> existing,
                                HashMap::new));
                Map<String, Village> villageMap = villageRepo.findAll().stream()
                        .collect(Collectors.toMap(
                                village -> key(village.getCell().getSector().getDistrict().getProvince().getName(), village.getCell().getSector().getDistrict().getName(), village.getCell().getSector().getName(), village.getCell().getName(), village.getName()),
                                village -> village,
                                (existing, duplicate) -> existing,
                                HashMap::new));

                for (Map<String, Object> loc : locations) {
                    String provName = stringValue(loc, "province_name");
                    String distName = stringValue(loc, "district_name");
                    String sectName = stringValue(loc, "sector_name");
                    String cellName = stringValue(loc, "cell_name");
                    String villName = stringValue(loc, "village_name");

                    if (provName == null || distName == null || sectName == null || cellName == null || villName == null) {
                        continue;
                    }

                    Province province = provinceMap.computeIfAbsent(provName, name -> provinceRepo.save(new Province(null, name)));
                    String districtKey = key(provName, distName);
                    District district = districtMap.computeIfAbsent(districtKey, k -> districtRepo.save(new District(null, distName, province)));

                    String sectorKey = key(provName, distName, sectName);
                    Sector sector = sectorMap.computeIfAbsent(sectorKey, k -> sectorRepo.save(new Sector(null, sectName, district)));

                    String cellKey = key(provName, distName, sectName, cellName);
                    Cell cell = cellMap.computeIfAbsent(cellKey, k -> cellRepo.save(new Cell(null, cellName, sector)));

                    String villageKey = key(provName, distName, sectName, cellName, villName);
                    villageMap.computeIfAbsent(villageKey, k -> villageRepo.save(new Village(null, villName, cell)));
                }

                System.out.println("Geography data successfully seeded!");
            } catch (Exception e) {
                System.out.println("Unable to save location data: " + e.getMessage());
                e.printStackTrace();
            }
        };
    }

    private String key(String... parts) {
        return String.join("|", Arrays.stream(parts)
                .map(this::normalize)
                .toArray(String[]::new));
    }

    private String stringValue(Map<String, Object> location, String key) {
        Object value = location.get(key);
        return value == null ? null : value.toString().trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}