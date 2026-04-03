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
            if (provinceRepo.count() == 0) {
                System.out.println("Seeding strict geographic data from locations.json...");
                ObjectMapper mapper = new ObjectMapper();

                // Note: Ensure the file path matches your resource location
                // Standard location is src/main/resources/locations.json
                TypeReference<List<Map<String, Object>>> typeReference = new TypeReference<>() {};
                InputStream inputStream = TypeReference.class.getResourceAsStream("/locations.json");

                if (inputStream == null) {
                    System.out.println("Error: locations.json not found in resources folder.");
                    return;
                }

                try {
                    List<Map<String, Object>> locations = mapper.readValue(inputStream, typeReference);

                    Map<String, Province> provinceMap = new HashMap<>();
                    Map<String, District> districtMap = new HashMap<>();
                    Map<String, Sector> sectorMap = new HashMap<>();
                    Map<String, Cell> cellMap = new HashMap<>();

                    for (Map<String, Object> loc : locations) {
                        // 1. Province
                        String provName = (String) loc.get("province_name");
                        Province province = provinceMap.computeIfAbsent(provName, k ->
                                provinceRepo.save(new Province(null, k)));

                        // 2. District
                        String distName = (String) loc.get("district_name");
                        // Use unique key combining parent + name to avoid collisions
                        String distKey = provName + "_" + distName;
                        District district = districtMap.computeIfAbsent(distKey, k ->
                                districtRepo.save(new District(null, distName, province)));

                        // 3. Sector
                        String sectName = (String) loc.get("sector_name");
                        String sectKey = distKey + "_" + sectName;
                        Sector sector = sectorMap.computeIfAbsent(sectKey, k ->
                                sectorRepo.save(new Sector(null, sectName, district)));

                        // 4. Cell
                        String cellName = (String) loc.get("cell_name");
                        String cellKey = sectKey + "_" + cellName;
                        Cell cell = cellMap.computeIfAbsent(cellKey, k ->
                                cellRepo.save(new Cell(null, cellName, sector)));

                        // 5. Village
                        String villName = (String) loc.get("village_name");
                        villageRepo.save(new Village(null, villName, cell));
                    }
                    System.out.println("Geography data successfully seeded!");
                } catch (Exception e) {
                    System.out.println("Unable to save location data: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Geography data already exists, skipping seeding.");
            }
        };
    }
}